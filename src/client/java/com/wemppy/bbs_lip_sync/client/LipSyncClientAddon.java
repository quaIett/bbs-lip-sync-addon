package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.LipSyncAddon;
import com.wemppy.bbs_lip_sync.client.settings.ValueSpeechLanguage;
import com.wemppy.bbs_lip_sync.client.settings.ValueTimingPreset;
import com.wemppy.bbs_lip_sync.client.ui.UILipSyncFormPanel;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.client.events.RegisterValueWidgetsEvent;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIBillboardForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIExtrudedForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * Client-side BBS addon entry point ({@code bbs-client-addon}): registers the language files, the
 * settings module, the settings widget of its own value type and the form editor tab. The timeline hook itself is a
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
        event.register(ValueTimingPreset.class, ValueTimingPreset::widget);
    }

    /**
     * There's no event for a tab in someone else's form editor, so the editors of the forms that
     * can have a mouth are registered again: BBS's own, with the «Lip Sync» tab added.
     */
    @Subscribe
    public void onRegisterFormEditors(RegisterFormEditorsEvent event)
    {
        event.register(ModelForm.class, () -> UILipSyncFormPanel.attach(new UIModelForm()));
        event.register(BillboardForm.class, () -> UILipSyncFormPanel.attach(new UIBillboardForm()));
        event.register(ExtrudedForm.class, () -> UILipSyncFormPanel.attach(new UIExtrudedForm()));
    }
}
