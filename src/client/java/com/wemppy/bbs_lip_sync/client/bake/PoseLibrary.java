package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A model's mouth poses from its {@code poses.json}: the pose picked for a shape in the mouth
 * profile, else the pose named like the shape ({@code idle}, {@code b}, {@code d}, {@code ee},
 * {@code a}, {@code o}, {@code u}, {@code f}, {@code l}, the convention of the bundled
 * {@code player/mouth} rig). A shape the rig still lacks borrows the nearest one it has;
 * {@code idle} falls back to the rest pose.
 */
public class PoseLibrary implements MouthRig<Pose>
{
    private final String modelId;
    private final Map<Viseme, String> names = new EnumMap<>(Viseme.class);
    private final Map<Viseme, Pose> poses = new EnumMap<>(Viseme.class);
    private final List<String> all = new ArrayList<>();

    private PoseLibrary(String modelId)
    {
        this.modelId = modelId;

        MapType data = PoseManager.INSTANCE.getData(modelId);
        Map<Viseme, String> picked = MouthProfile.poses(modelId);
        Map<Viseme, Pose> own = new EnumMap<>(Viseme.class);

        this.all.addAll(data.keys());

        for (Viseme viseme : Viseme.values())
        {
            String name = find(data, picked.getOrDefault(viseme, viseme.id));
            BaseType entry = name == null ? null : data.get(name);

            if (entry != null && entry.isMap())
            {
                own.put(viseme, KeyframeFactories.POSE.fromData(entry));
                this.names.put(viseme, name);
            }
        }

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

    @Override
    public boolean isUsable()
    {
        return this.names.size() >= 2;
    }

    @Override
    public Pose value(Viseme viseme)
    {
        return this.poses.get(viseme);
    }

    @Override
    public boolean isSmooth()
    {
        return true;
    }

    @Override
    public String source()
    {
        return this.modelId;
    }

    /** The pose a shape was found as, picked or by its name; null when it borrows another shape's. */
    public String name(Viseme viseme)
    {
        return this.names.get(viseme);
    }

    /** Every pose in the model's {@code poses.json}. */
    public List<String> all()
    {
        return this.all;
    }

    /** The key of the pose called {@code name}, ignoring case; null if there's none. */
    private static String find(MapType data, String name)
    {
        if (data.has(name))
        {
            return name;
        }

        for (String key : data.keys())
        {
            if (key.equalsIgnoreCase(name))
            {
                return key;
            }
        }

        return null;
    }
}
