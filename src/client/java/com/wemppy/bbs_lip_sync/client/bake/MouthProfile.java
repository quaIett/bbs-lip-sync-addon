package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@code lipsync.json}: which pose or picture stands for a mouth shape when the names alone don't
 * say. One format wherever it lies:
 *
 * <pre>
 * {
 *     "poses": {"a": "open", "o": "round"},
 *     "textures": {"skin.png": {"a": "skin_a.png"}}
 * }
 * </pre>
 *
 * <p>{@code poses} is read in a model's folder, next to its {@code poses.json}. {@code textures}
 * is read in the folder of the pictures, keyed by the closed mouth picture (the form's own
 * texture), so several characters' mouths can share a folder. A picture from that folder is kept
 * by its file name, one from anywhere else by its full link. Only the shapes the user picked are
 * stored; the rest are found by name.</p>
 *
 * <p>Nothing is cached: the file is tiny and read when the tab opens or a generation starts, so
 * an edit by hand counts without a restart. Like {@code poses.json}, writes land in the assets
 * folder even for models bundled with BBS.</p>
 */
public class MouthProfile
{
    public static final String FILE = "lipsync.json";

    private static final String POSES = "poses";
    private static final String TEXTURES = "textures";

    /** The pose names the user picked for a model's shapes. */
    public static Map<Viseme, String> poses(String modelId)
    {
        Map<Viseme, String> poses = new EnumMap<>(Viseme.class);
        MapType section = read(modelFile(modelId)).getMap(POSES);

        for (Viseme viseme : Viseme.values())
        {
            String pose = section.getString(viseme.id, "");

            if (!pose.isEmpty())
            {
                poses.put(viseme, pose);
            }
        }

        return poses;
    }

    /** @param pose null or empty to forget the choice */
    public static void setPose(String modelId, Viseme viseme, String pose)
    {
        Link file = modelFile(modelId);
        MapType data = read(file);
        MapType section = data.getMap(POSES);

        if (pose == null || pose.isEmpty())
        {
            section.remove(viseme.id);
        }
        else
        {
            section.putString(viseme.id, pose);
        }

        put(data, POSES, section);
        write(file, data);
    }

    /** The pictures the user picked for the shapes of the mouth whose closed picture is {@code closed}. */
    public static Map<Viseme, Link> textures(Link closed)
    {
        Map<Viseme, Link> textures = new EnumMap<>(Viseme.class);
        MapType section = read(folderFile(closed)).getMap(TEXTURES).getMap(fileName(closed));

        for (Viseme viseme : Viseme.values())
        {
            String texture = section.getString(viseme.id, "");

            if (!texture.isEmpty())
            {
                textures.put(viseme, resolve(closed, texture));
            }
        }

        return textures;
    }

    /** @param texture null to forget the choice */
    public static void setTexture(Link closed, Viseme viseme, Link texture)
    {
        Link file = folderFile(closed);
        MapType data = read(file);
        MapType textures = data.getMap(TEXTURES);
        MapType section = textures.getMap(fileName(closed));

        if (texture == null)
        {
            section.remove(viseme.id);
        }
        else
        {
            section.putString(viseme.id, relative(closed, texture));
        }

        put(textures, fileName(closed), section);
        put(data, TEXTURES, textures);
        write(file, data);
    }

    public static String fileName(Link link)
    {
        int slash = link.path.lastIndexOf('/');

        return slash < 0 ? link.path : link.path.substring(slash + 1);
    }

    private static Link modelFile(String modelId)
    {
        return Link.assets(ModelManager.MODELS_PREFIX + modelId + "/" + FILE);
    }

    private static Link folderFile(Link closed)
    {
        return closed.parent().combine(FILE);
    }

    /** A stored picture: a bare file name sits next to the closed one, anything else is a full link. */
    private static Link resolve(Link closed, String texture)
    {
        return texture.contains(":") ? Link.create(texture) : closed.parent().combine(texture);
    }

    private static String relative(Link closed, Link texture)
    {
        Link folder = closed.parent();

        return texture.source.equals(folder.source) && folder.combine(fileName(texture)).path.equals(texture.path)
            ? fileName(texture)
            : texture.toString();
    }

    /** An empty section is dropped rather than left behind as {@code {}}. */
    private static void put(MapType parent, String key, MapType section)
    {
        if (section.isEmpty())
        {
            parent.remove(key);
        }
        else
        {
            parent.put(key, section);
        }
    }

    private static MapType read(Link file)
    {
        try (InputStream stream = BBSMod.getProvider().getAsset(file))
        {
            MapType data = DataToString.mapFromString(IOUtils.readText(stream));

            return data == null ? new MapType() : data;
        }
        catch (FileNotFoundException e)
        {
            return new MapType();
        }
        catch (Exception e)
        {
            System.err.println("[bbs_lip_sync] Failed to read " + file);
            e.printStackTrace();

            return new MapType();
        }
    }

    private static void write(Link link, MapType data)
    {
        File file = BBSMod.getProvider().getFile(link);

        if (file == null)
        {
            System.err.println("[bbs_lip_sync] Nowhere to save " + link);

            return;
        }

        if (data.isEmpty())
        {
            file.delete();

            return;
        }

        file.getParentFile().mkdirs();
        DataToString.writeSilently(file, data, true);
    }
}
