package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.client.phonemes.TextSpeech;
import com.wemppy.bbs_lip_sync.client.settings.ValueSpeechLanguage;
import com.wemppy.bbs_lip_sync.client.settings.ValueTimingPreset;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * The addon's settings module ({@code config/bbs/settings/bbs_lip_sync.json}). Titles and
 * tooltips come from {@code bbs_lip_sync.config.*} language keys.
 */
public class LipSyncSettings
{
    public static final int MODEL_SMALL = 0;
    public static final int MODEL_LARGE = 1;

    /** Language code of the speech, e.g. {@code ru}; empty means the game's language. */
    public static ValueString language;

    /** {@link #MODEL_SMALL} or {@link #MODEL_LARGE} (used when the language has one). */
    public static ValueInt modelSize;

    /** Free the speech model's memory once a generation is done. */
    public static ValueBoolean unloadModel;

    /** Calm, ordinary or fast speech: sets the four values below at once. */
    public static ValueTimingPreset timingPreset;

    /** How many ticks before a sound the mouth starts moving toward its shape. */
    public static ValueFloat anticipation;

    /** Weak consonant shapes shorter than this (in ticks) dissolve into the neighbouring vowel. */
    public static ValueFloat minHold;

    /** A silence has to last this long (in ticks) before the mouth closes. */
    public static ValueFloat minPause;

    /** How long (in ticks) the mouth keeps its last shape after a word before it closes. */
    public static ValueFloat closeDelay;

    /** Speech speed of lip sync from text, 1 being ordinary speech. Set in the text dialog, so hidden here. */
    public static ValueFloat textSpeed;

    public static void register(SettingsBuilder builder)
    {
        builder.category("speech", Icons.BUBBLE);
        language = new ValueSpeechLanguage("language", "");
        builder.register(language);
        modelSize = builder.getInt("model_size", MODEL_SMALL, MODEL_SMALL, MODEL_LARGE).modes(LipSyncKeys.CONFIG_MODEL_SMALL, LipSyncKeys.CONFIG_MODEL_LARGE);
        unloadModel = builder.getBoolean("unload_model", true);

        builder.category("timing", Icons.STOPWATCH);
        timingPreset = new ValueTimingPreset("preset");
        builder.register(timingPreset);
        anticipation =builder.getFloat("anticipation", 1F, 0F, 3F).slider(0.25D);
        minHold = builder.getFloat("min_hold", 1F, 0.5F, 5F).slider(0.25D);
        minPause = builder.getFloat("min_pause", 10F, 3F, 40F).slider(1D);
        closeDelay = builder.getFloat("close_delay", 4F, 0F, 10F).slider(0.5D);
        textSpeed = builder.getFloat("text_speed", 1F, TextSpeech.MIN_SPEED, TextSpeech.MAX_SPEED);
        textSpeed.invisible();
    }

    public static String language()
    {
        return language == null ? "" : language.get();
    }

    public static boolean largeModel()
    {
        return modelSize != null && modelSize.get() == MODEL_LARGE;
    }

    public static boolean unloadModel()
    {
        return unloadModel == null || unloadModel.get();
    }

    public static float anticipation()
    {
        return anticipation == null ? 1F : anticipation.get();
    }

    public static float minHold()
    {
        return minHold == null ? 1F : minHold.get();
    }

    public static float minPause()
    {
        return minPause == null ? 10F : minPause.get();
    }

    public static float closeDelay()
    {
        return closeDelay == null ? 4F : closeDelay.get();
    }

    public static float textSpeed()
    {
        return textSpeed == null ? 1F : TextSpeech.clampSpeed(textSpeed.get());
    }
}
