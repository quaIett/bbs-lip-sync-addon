package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.LipSyncAddon;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Where the addon's own data lives: bundled assets, and the user's folder next to BBS's config. */
public class Bundled
{
    /** Text of a bundled asset, e.g. {@code letters/ru.json}. */
    public static String read(String path) throws IOException
    {
        Link link = new Link(LipSyncAddon.MOD_ID, path);

        try (InputStream stream = BBSMod.getProvider().getAsset(link))
        {
            if (stream == null)
            {
                throw new IOException("Missing asset " + link);
            }

            return IOUtils.readText(stream);
        }
    }

    /** {@code config/bbs/lipsync}: downloaded models and the user's overrides. */
    public static File folder()
    {
        return new File(BBSMod.getSettingsFolder().getParentFile(), "lipsync");
    }
}
