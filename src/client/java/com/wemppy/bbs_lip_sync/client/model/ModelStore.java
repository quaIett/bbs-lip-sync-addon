package com.wemppy.bbs_lip_sync.client.model;

import com.wemppy.bbs_lip_sync.client.Bundled;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Where Vosk models live on disk and how they get there. Each model is a folder under
 * {@code config/bbs/lipsync/models}, named like the zip it came from; a download fetches the zip,
 * unpacks it and deletes it. One download runs at a time.
 */
public class ModelStore
{
    public enum State
    {
        IDLE, DOWNLOADING, EXTRACTING, ERROR
    }

    public enum Error
    {
        NETWORK, DISK, CORRUPT
    }

    /** The file every Vosk model has; its presence is what makes a folder count as a model. */
    private static final String MARKER = "am/final.mdl";

    private static volatile State state = State.IDLE;
    private static volatile Error error;
    private static volatile Language.Model target;
    private static volatile long received;
    private static volatile long total = -1;
    private static volatile long speed;
    private static volatile boolean cancel;

    /** The one thing waiting for a model: the latest request replaces an earlier, stale one. */
    private static volatile Runnable onReady;

    public static File folder()
    {
        return new File(Bundled.folder(), "models");
    }

    public static File dir(Language.Model model)
    {
        return new File(folder(), model.name());
    }

    public static boolean isReady(Language.Model model)
    {
        return model != null && new File(dir(model), MARKER).isFile();
    }

    public static State state()
    {
        return state;
    }

    public static Error error()
    {
        return error;
    }

    /** The model being (or last) downloaded. */
    public static Language.Model target()
    {
        return target;
    }

    public static long received()
    {
        return received;
    }

    /** Size of the download in bytes, or -1 while unknown. */
    public static long total()
    {
        return total;
    }

    /** Bytes per second over the last second of the download. */
    public static long speed()
    {
        return speed;
    }

    /** Runs once, on the main thread, when a download finishes successfully; replaces any earlier request. */
    public static void onceReady(Runnable runnable)
    {
        onReady = runnable;
    }

    /** Runs the waiting request now (on the main thread), e.g. because the model turned out to be there already. */
    public static void fireReady()
    {
        Runnable callback = onReady;

        onReady = null;

        if (callback != null)
        {
            MinecraftClient.getInstance().execute(callback);
        }
    }

    public static synchronized void download(Language.Model model)
    {
        if (state == State.DOWNLOADING || state == State.EXTRACTING)
        {
            return;
        }

        state = State.DOWNLOADING;
        error = null;
        target = model;
        cancel = false;
        received = 0;
        total = -1;
        speed = 0;

        Thread thread = new Thread(() -> run(model), "BBS lipsync model download");

        thread.setDaemon(true);
        thread.start();
    }

    public static void cancelDownload()
    {
        cancel = true;
    }

    /** Forgets a failed download so the dialog shows the plain offer again. */
    public static void clearError()
    {
        if (state == State.ERROR)
        {
            state = State.IDLE;
            error = null;
        }
    }

    private static void run(Language.Model model)
    {
        File zip = new File(folder(), model.name() + ".zip.part");
        File temp = new File(folder(), model.name() + ".tmp");

        try
        {
            folder().mkdirs();
            fetch(model.url(), zip);

            if (cancel)
            {
                state = State.IDLE;

                return;
            }

            state = State.EXTRACTING;
            unpack(zip, temp);
            install(temp, dir(model));

            state = State.IDLE;

            fireReady();
        }
        catch (Failure e)
        {
            e.printStackTrace();
            fail(e.error);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(Error.NETWORK);
        }
        finally
        {
            zip.delete();
            deleteTree(temp);
        }
    }

    private static void fetch(String url, File zip) throws Exception
    {
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "bbs-lip-sync")
            .timeout(Duration.ofHours(6))
            .build();
        HttpResponse<InputStream> response;

        try
        {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        }
        catch (Exception e)
        {
            throw new Failure(Error.NETWORK, e);
        }

        if (response.statusCode() != 200)
        {
            throw new Failure(Error.NETWORK, new IOException("HTTP " + response.statusCode()));
        }

        total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(zip.toPath()))
        {
            byte[] buffer = new byte[1 << 16];
            long windowStart = System.currentTimeMillis();
            long windowBytes = 0;
            int read;

            while (true)
            {
                try
                {
                    read = in.read(buffer);
                }
                catch (IOException e)
                {
                    throw new Failure(Error.NETWORK, e);
                }

                if (read <= 0)
                {
                    break;
                }

                if (cancel)
                {
                    return;
                }

                try
                {
                    out.write(buffer, 0, read);
                }
                catch (IOException e)
                {
                    throw new Failure(Error.DISK, e);
                }

                received += read;
                windowBytes += read;

                long now = System.currentTimeMillis();

                if (now - windowStart >= 1000)
                {
                    speed = windowBytes * 1000 / (now - windowStart);
                    windowStart = now;
                    windowBytes = 0;
                }
            }
        }
        catch (Failure e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new Failure(Error.DISK, e);
        }

        if (total >= 0 && received != total)
        {
            throw new Failure(Error.NETWORK, new IOException("Truncated download: " + received + " of " + total));
        }
    }

    /** Unpacks the zip into {@code into}, refusing entries that would escape it. */
    private static void unpack(File zip, File into) throws Exception
    {
        deleteTree(into);
        into.mkdirs();

        Path root = into.toPath().toAbsolutePath().normalize();

        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip.toPath())))
        {
            ZipEntry entry;
            byte[] buffer = new byte[1 << 16];

            while ((entry = in.getNextEntry()) != null)
            {
                Path path = root.resolve(entry.getName()).normalize();

                if (!path.startsWith(root))
                {
                    throw new Failure(Error.CORRUPT, new ZipException("Bad entry " + entry.getName()));
                }

                if (entry.isDirectory())
                {
                    Files.createDirectories(path);

                    continue;
                }

                Files.createDirectories(path.getParent());

                try (OutputStream out = Files.newOutputStream(path))
                {
                    int read;

                    while ((read = in.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
        catch (Failure e)
        {
            throw e;
        }
        catch (ZipException e)
        {
            throw new Failure(Error.CORRUPT, e);
        }
        catch (IOException e)
        {
            throw new Failure(Error.DISK, e);
        }
    }

    /** Moves the model folder found inside the unpacked tree to its final place. */
    private static void install(File temp, File target) throws Exception
    {
        File found = findModel(temp, 3);

        if (found == null)
        {
            throw new Failure(Error.CORRUPT, new IOException("No " + MARKER + " in the archive"));
        }

        deleteTree(target);

        try
        {
            Files.move(found.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException e)
        {
            try
            {
                Files.move(found.toPath(), target.toPath());
            }
            catch (IOException e2)
            {
                throw new Failure(Error.DISK, e2);
            }
        }
    }

    private static File findModel(File folder, int depth)
    {
        if (new File(folder, MARKER).isFile())
        {
            return folder;
        }

        File[] children = depth > 0 ? folder.listFiles(File::isDirectory) : null;

        if (children != null)
        {
            for (File child : children)
            {
                File found = findModel(child, depth - 1);

                if (found != null)
                {
                    return found;
                }
            }
        }

        return null;
    }

    private static void deleteTree(File folder)
    {
        if (!folder.exists())
        {
            return;
        }

        try (Stream<Path> walk = Files.walk(folder.toPath()))
        {
            walk.sorted(Comparator.reverseOrder()).forEach((path) -> path.toFile().delete());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void fail(Error reason)
    {
        error = reason;
        state = State.ERROR;
        speed = 0;
    }

    private static class Failure extends Exception
    {
        final Error error;

        Failure(Error error, Throwable cause)
        {
            super(error.name(), cause);

            this.error = error;
        }
    }
}
