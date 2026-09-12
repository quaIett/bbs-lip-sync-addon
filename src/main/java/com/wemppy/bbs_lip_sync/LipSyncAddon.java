package com.wemppy.bbs_lip_sync;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;

/**
 * Common-side BBS addon entry point ({@code bbs-addon}). Everything the addon does lives on the
 * client; this class only claims the {@code bbs_lip_sync:} asset source so the client can read
 * its own strings and model tables through BBS's asset provider.
 */
public class LipSyncAddon implements BBSAddonMod
{
    public static final String MOD_ID = "bbs_lip_sync";

    public LipSyncAddon()
    {
        BBSApi.requireVersion(MOD_ID, 1);
    }

    @Subscribe
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon(MOD_ID, LipSyncAddon.class);
    }
}
