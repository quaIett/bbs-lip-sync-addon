package com.wemppy.bbs_lip_sync.client.settings;

import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import com.wemppy.bbs_lip_sync.client.model.Language;
import com.wemppy.bbs_lip_sync.client.model.Languages;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.utils.UIConstants;

import java.util.Arrays;
import java.util.List;

/**
 * The language spoken in the audio, stored as its code ({@code ru}, {@code en}; empty for the
 * game's language). Its own class only so the settings screen shows the languages to click
 * through, like any other BBS mode switch, instead of a text box for a code.
 */
public class ValueSpeechLanguage extends ValueString
{
    public ValueSpeechLanguage(String id, String defaultValue)
    {
        super(id, defaultValue);
    }

    /** The settings row: "game language" followed by every language, left click forward, right click back. */
    public static List<UIElement> widget(ValueSpeechLanguage value, UIElement ui)
    {
        List<Language> languages = Languages.all();
        UICirculate button = new UICirculate(null);

        button.addLabel(LipSyncKeys.CONFIG_LANGUAGE_GAME);

        for (Language language : languages)
        {
            button.addLabel(IKey.raw(language.name));
        }

        button.callback = (b) ->
        {
            int index = button.getValue();

            value.set(index <= 0 || index > languages.size() ? "" : languages.get(index - 1).code);
        };
        button.valueBinding(() -> button.setValue(index(value.get(), languages)));
        button.setValue(index(value.get(), languages));
        button.w(UIConstants.VALUE_WIDTH);

        return Arrays.asList(UIValueFactory.column(button, value));
    }

    /** Position of a code in the button's labels: 0 is the game's language. */
    private static int index(String code, List<Language> languages)
    {
        if (code == null || code.isEmpty())
        {
            return 0;
        }

        for (int i = 0; i < languages.size(); i++)
        {
            if (languages.get(i).code.equalsIgnoreCase(code.trim()))
            {
                return i + 1;
            }
        }

        return 0;
    }
}
