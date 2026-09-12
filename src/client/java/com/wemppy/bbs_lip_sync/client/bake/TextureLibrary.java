package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.resources.MultiLink;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The mouth pictures of a form that animates its mouth by swapping textures. The closed mouth is
 * the form's own texture (for a model form without one, its model's), and every other shape is
 * the picture picked in the mouth profile, else the one named after the closed picture and the
 * shape in the same folder ({@code wemppy.png} or {@code wemppy_idle.png} → {@code wemppy_a.png}),
 * else plain {@code a.png}, the way the old lip sync tool named them. A shape still missing
 * borrows a similar one, as with poses.
 */
public class TextureLibrary implements MouthRig<Link>
{
    private final Link closed;
    private final Map<Viseme, Link> own = new EnumMap<>(Viseme.class);
    private final Map<Viseme, Link> textures = new EnumMap<>(Viseme.class);

    private TextureLibrary(Link closed)
    {
        this.closed = closed;

        Map<Viseme, Link> picked = MouthProfile.textures(closed);
        Map<String, Link> files = files(closed.parent());

        for (Viseme viseme : Viseme.values())
        {
            Link texture = picked.get(viseme);

            if (texture == null)
            {
                texture = viseme == Viseme.IDLE ? closed : byName(files, viseme);
            }

            if (texture != null)
            {
                this.own.put(viseme, texture);
            }
        }

        for (Viseme viseme : Viseme.values())
        {
            Link texture = this.own.get(viseme);

            if (texture == null)
            {
                for (Viseme fallback : viseme.fallbacks())
                {
                    texture = this.own.get(fallback);

                    if (texture != null)
                    {
                        break;
                    }
                }
            }

            this.textures.put(viseme, texture == null ? this.own.get(Viseme.IDLE) : texture);
        }
    }

    public static TextureLibrary of(Link closed)
    {
        return new TextureLibrary(closed);
    }

    /**
     * The closed mouth picture of a form: its own texture, or for a model form without one, the
     * model's. Null when there's none, or when it's a multi-texture, which isn't supported.
     */
    public static Link closedTexture(Form form)
    {
        Link link = form.getBasic("texture") instanceof ValueLink value ? value.getOriginalValue() : null;

        if (link == null && form instanceof ModelForm model)
        {
            ModelInstance instance = ModelFormRenderer.getModel(model);

            link = instance == null ? null : instance.getTexture();
        }

        return link instanceof MultiLink ? null : link;
    }

    @Override
    public boolean isUsable()
    {
        return this.own.size() >= 2;
    }

    @Override
    public Link value(Viseme viseme)
    {
        return this.textures.get(viseme);
    }

    @Override
    public boolean isSmooth()
    {
        return false;
    }

    @Override
    public String source()
    {
        return this.closed.toString();
    }

    public Link closed()
    {
        return this.closed;
    }

    /** The picture a shape was found as, picked or by its name; null when it borrows another shape's. */
    public Link texture(Viseme viseme)
    {
        return this.own.get(viseme);
    }

    private Link byName(Map<String, Link> files, Viseme viseme)
    {
        String name = MouthProfile.fileName(this.closed);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? ".png" : name.substring(dot);
        String stem = (dot < 0 ? name : name.substring(0, dot)).toLowerCase();

        if (stem.equals(Viseme.IDLE.id))
        {
            stem = "";
        }
        else if (stem.endsWith("_" + Viseme.IDLE.id))
        {
            stem = stem.substring(0, stem.length() - Viseme.IDLE.id.length() - 1);
        }

        Link texture = stem.isEmpty() ? null : files.get(stem + "_" + viseme.id + extension.toLowerCase());

        return texture != null ? texture : files.get(viseme.id + extension.toLowerCase());
    }

    /** The files right inside a folder, by lower case name. */
    private static Map<String, Link> files(Link folder)
    {
        Map<String, Link> files = new HashMap<>();

        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(folder, false))
            {
                files.putIfAbsent(MouthProfile.fileName(link).toLowerCase(), link);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return files;
    }
}
