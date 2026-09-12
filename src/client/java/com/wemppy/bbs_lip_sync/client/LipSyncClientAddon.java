package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.LipSyncAddon;
import com.wemppy.bbs_lip_sync.client.settings.ValueSpeechLanguage;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.client.events.RegisterValueWidgetsEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * Client-side BBS addon entry point ({@code bbs-client-addon}): registers the language files, the
 * settings module and the settings widget of its own value type. The timeline hook itself is a
 * mixin, see
 * {@link com.wemppy.bbs_lip_sync.mixin.client.UIReplaysEditorMixin}.
 */
public class LipSyncClientAddon implements BBSAddonMod
{
    @Subscribe
    public void onRegisterL10n(RegisterL10nEvent event)
    {
        event.l10n.registerOne((lang) -> new Link(LipSyncAddon.MOD_ID, "strings/" + lang + ".json"));
    }

    @Subscribe
    public void onRegisterSettings(RegisterClientSettingsEvent event)
    {
        event.register(Icons.VOICE, LipSyncAddon.MOD_ID, LipSyncSettings::register);
    }

    @Subscribe
    public void onRegisterValueWidgets(RegisterValueWidgetsEvent event)
    {
        event.register(ValueSpeechLanguage.class, ValueSpeechLanguage::widget);
    }
}
