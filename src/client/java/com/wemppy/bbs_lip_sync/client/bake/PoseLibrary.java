package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * Finds a model's mouth poses in its {@code poses.json} by the viseme names ({@code idle},
 * {@code b}, {@code d}, {@code ee}, {@code a}, {@code o}, {@code u}, {@code f}, {@code l}), the
 * convention the bundled {@code player/mouth} rig follows. A shape the rig lacks borrows the
 * nearest one it has; {@code idle} falls back to the rest pose.
 */
public class PoseLibrary
{
    private final Map<Viseme, Pose> poses = new EnumMap<>(Viseme.class);
    private final int found;

    private PoseLibrary(String modelId)
    {
        MapType data = PoseManager.INSTANCE.getData(modelId);
        Map<Viseme, Pose> own = new EnumMap<>(Viseme.class);

        for (Viseme viseme : Viseme.values())
        {
            BaseType entry = find(data, viseme.id);

            if (entry != null && entry.isMap())
            {
                own.put(viseme, KeyframeFactories.POSE.fromData(entry));
            }
        }

        this.found = own.size();

        for (Viseme viseme : Viseme.values())
        {
            Pose pose = own.get(viseme);

            if (pose == null)
            {
                for (Viseme fallback : viseme.fallbacks())
                {
                    pose = own.get(fallback);

                    if (pose != null)
                    {
                        break;
                    }
                }
            }

            if (pose == null)
            {
                pose = own.getOrDefault(Viseme.IDLE, new Pose());
            }

            this.poses.put(viseme, pose);
        }
    }

    public static PoseLibrary of(String modelId)
    {
        return new PoseLibrary(modelId);
    }

    /** Whether the rig has enough named poses to animate speech at all. */
    public boolean isUsable()
    {
        return this.found >= 2;
    }

    public Pose pose(Viseme viseme)
    {
        return this.poses.get(viseme);
    }

    private static BaseType find(MapType data, String name)
    {
        if (data.has(name))
        {
            return data.get(name);
        }

        for (String key : data.keys())
        {
            if (key.equalsIgnoreCase(name))
            {
                return data.get(key);
            }
        }

        return null;
    }
}
