package com.wemppy.bbs_lip_sync.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Turns audio into words with timestamps using Vosk (Kaldi). One model stays loaded at a time;
 * loading takes a moment for the small models and a good while for the large ones, so the model
 * is kept between generations unless the settings say to drop it.
 */
public class SpeechRecognizer
{
    public static final int SAMPLE_RATE = 16000;

    /** Audio is fed in quarter-second pieces, between which progress is reported and cancellation checked. */
    private static final int CHUNK = SAMPLE_RATE / 4;

    private static SpeechRecognizer instance;

    private final File folder;
    private final Model model;

    private SpeechRecognizer(File folder, Model model)
    {
        this.folder = folder;
        this.model = model;
    }

    /**
     * The recognizer for a model folder, loading it if it isn't the one already loaded.
     *
     * @throws IOException if Vosk can't load the model (missing or broken files)
     */
    public static synchronized SpeechRecognizer get(File folder) throws IOException
    {
        if (instance != null && !instance.folder.equals(folder))
        {
            unload();
        }

        if (instance == null)
        {
            LibVosk.setLogLevel(LogLevel.WARNINGS);

            instance = new SpeechRecognizer(folder, new Model(nativePath(folder)));
        }

        return instance;
    }

    public static synchronized void unload()
    {
        if (instance != null)
        {
            instance.model.close();
            instance = null;
        }
    }

    /**
     * Vosk opens files through the C library, which on Windows can't take a path with characters
     * outside the system code page (a Cyrillic user name, say). The models sit under the game
     * folder, so a path relative to it — plain ASCII — sidesteps that.
     */
    private static String nativePath(File folder)
    {
        try
        {
            Path cwd = Paths.get("").toAbsolutePath().normalize();
            Path target = folder.toPath().toAbsolutePath().normalize();

            if (target.startsWith(cwd))
            {
                return cwd.relativize(target).toString();
            }
        }
        catch (Exception e)
        {
            /* Fall through to the absolute path */
        }

        return folder.getAbsolutePath();
    }

    /**
     * @param samples   16 kHz mono in [-1, 1]
     * @param progress  gets 0..100
     * @param cancelled polled between chunks
     * @return the words heard, in order; null if cancelled
     */
    public List<Word> recognize(float[] samples, IntConsumer progress, BooleanSupplier cancelled) throws IOException
    {
        List<Word> words = new ArrayList<>();

        try (Recognizer recognizer = new Recognizer(this.model, SAMPLE_RATE))
        {
            recognizer.setWords(true);

            float[] chunk = new float[CHUNK];

            for (int offset = 0; offset < samples.length; offset += CHUNK)
            {
                if (cancelled.getAsBoolean())
                {
                    return null;
                }

                int length = Math.min(CHUNK, samples.length - offset);

                for (int i = 0; i < length; i++)
                {
                    /* Vosk expects 16-bit scale even for floats */
                    chunk[i] = samples[offset + i] * 32767F;
                }

                if (recognizer.acceptWaveForm(chunk, length))
                {
                    parse(result(recognizer, "vosk_recognizer_result"), words);
                }

                progress.accept((int) ((offset + length) * 100L / Math.max(1, samples.length)));
            }

            parse(result(recognizer, "vosk_recognizer_final_result"), words);
        }

        return words;
    }

    /**
     * Vosk's results are UTF-8, but the Java binding lets JNA decode them with the system code
     * page (windows-1251 on a Russian Windows, say), which garbles every non-ASCII word. So the
     * result is fetched as a raw pointer from the already loaded library and decoded here.
     */
    private static String result(Recognizer recognizer, String function)
    {
        NativeLibrary library = NativeLibrary.getInstance(Platform.isWindows() ? "libvosk" : "vosk", Collections.singletonMap(Library.OPTION_CLASSLOADER, LibVosk.class.getClassLoader()));
        Function call = library.getFunction(function);
        Pointer pointer = call.invokePointer(new Object[] {recognizer.getPointer()});

        return pointer == null ? null : pointer.getString(0, "UTF-8");
    }

    /** Vosk's result JSON: {@code {"result": [{"word": ..., "start": ..., "end": ..., "conf": ...}], "text": ...}}. */
    static void parse(String json, List<Word> words)
    {
        if (json == null || json.isEmpty())
        {
            return;
        }

        JsonElement root = JsonParser.parseString(json);

        if (!root.isJsonObject() || !root.getAsJsonObject().has("result"))
        {
            return;
        }

        JsonArray result = root.getAsJsonObject().getAsJsonArray("result");

        for (JsonElement element : result)
        {
            JsonObject object = element.getAsJsonObject();
            String text = object.has("word") ? object.get("word").getAsString().trim() : "";
            float start = object.get("start").getAsFloat();
            float end = object.get("end").getAsFloat();

            if (!text.isEmpty() && end > start)
            {
                words.add(new Word(text, start, end));
            }
        }
    }
}
