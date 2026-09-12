package com.wemppy.bbs_lip_sync.client.settings;

import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import com.wemppy.bbs_lip_sync.client.model.Language;
import com.wemppy.bbs_lip_sync.client.model.Languages;
import com.wemppy.bbs_lip_sync.client.ui.LanguagePicker;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.UIConstants;

import java.util.Arrays;
import java.util.List;

/**
 * The language spoken in the audio, stored as its code ({@code ru}, {@code en}; empty for the
 * game's language). Its own class only so the settings screen shows a button that opens the same
 * searchable language list as the track's context menu, instead of a text box for a code.
 */
public class ValueSpeechLanguage extends ValueString
{
    public ValueSpeechLanguage(String id, String defaultValue)
    {
        super(id, defaultValue);
    }

    /** The settings row: a button named after the current language; right click resets it to the game's. */
    public static List<UIElement> widget(ValueSpeechLanguage value, UIElement ui)
    {
        UIButton button = new UIButton(LipSyncKeys.CONFIG_LANGUAGE_GAME, (b) ->
        {
            if (b.getContext() != null)
            {
                LanguagePicker.open(b.getContext(), (picked) -> {});
            }
        });

        /* Read on every frame, so a pick or a reset shows up without rebuilding the row */
        button.label = () -> label(value.get());
        button.w(UIConstants.VALUE_WIDTH);

        return Arrays.asList(UIValueFactory.column(button, value));
    }

    private static String label(String code)
    {
        Language language = code == null || code.isEmpty() ? null : Languages.byCode(code);

        return language == null ? LipSyncKeys.CONFIG_LANGUAGE_GAME.get() : language.name;
    }
}
