package com.wemppy.bbs_lip_sync.client.settings;

import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import com.wemppy.bbs_lip_sync.client.LipSyncSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.utils.UIConstants;

import java.util.Arrays;
import java.util.List;

/**
 * The timing preset: calm, ordinary or fast speech, each a set of values for the four timing
 * sliders under it. Picking one moves the sliders; moving a slider by hand makes the preset read
 * «Custom». What is shown is always worked out from the sliders, so the stored number only
 * remembers the last preset picked.
 */
public class ValueTimingPreset extends ValueInt
{
    /**
     * The close delay counts toward whether a pause closes the mouth at all (see
     * VisemeTimeline), so calm speech shortens the pause and keeps the delay near ordinary, or the
     * mouth would close no more often than with the ordinary preset.
     */
    public enum Preset
    {
        CALM(1.25F, 0.75F, 7F, 4.5F),
        NORMAL(1F, 1F, 10F, 4F),
        FAST(0.75F, 1.5F, 14F, 3F);

        public final float anticipation;
        public final float minHold;
        public final float minPause;
        public final float closeDelay;

        Preset(float anticipation, float minHold, float minPause, float closeDelay)
        {
            this.anticipation = anticipation;
            this.minHold = minHold;
            this.minPause = minPause;
            this.closeDelay = closeDelay;
        }

        public void apply()
        {
            LipSyncSettings.anticipation.set(this.anticipation);
            LipSyncSettings.minHold.set(this.minHold);
            LipSyncSettings.minPause.set(this.minPause);
            LipSyncSettings.closeDelay.set(this.closeDelay);
        }

        /** Whether the sliders hold exactly this preset's values. */
        public boolean matches()
        {
            return same(LipSyncSettings.anticipation(), this.anticipation)
                && same(LipSyncSettings.minHold(), this.minHold)
                && same(LipSyncSettings.minPause(), this.minPause)
                && same(LipSyncSettings.closeDelay(), this.closeDelay);
        }

        private static boolean same(float a, float b)
        {
            return Math.abs(a - b) < 0.001F;
        }
    }

    public ValueTimingPreset(String id)
    {
        super(id, Preset.NORMAL.ordinal(), 0, Preset.values().length - 1);
    }

    /** The preset the sliders match, or null when they were set by hand. */
    public static Preset current()
    {
        for (Preset preset : Preset.values())
        {
            if (preset.matches())
            {
                return preset;
            }
        }

        return null;
    }

    /** Right click on the row brings the ordinary preset back, sliders included. */
    @Override
    public void reset()
    {
        super.reset();

        Preset.NORMAL.apply();
    }

    /** The settings row: the usual mode switch, labelled after whatever the sliders match. */
    public static List<UIElement> widget(ValueTimingPreset value, UIElement ui)
    {
        UICirculate circulate = new UICirculate((b) ->
        {
            Preset preset = Preset.values()[b.getValue()];

            value.set(preset.ordinal());
            preset.apply();
        });

        for (Preset preset : Preset.values())
        {
            circulate.addLabel(LipSyncKeys.timingPreset(preset));
        }

        /* Read on every frame, so a slider moved by hand shows up as «Custom» right away */
        circulate.valueBinding(() ->
        {
            Preset preset = current();

            if (preset != null)
            {
                circulate.setValue(preset.ordinal());
            }
            else
            {
                circulate.label = LipSyncKeys.TIMING_CUSTOM;
            }
        });
        circulate.w(UIConstants.VALUE_WIDTH);

        return Arrays.asList(UIValueFactory.column(circulate, value));
    }
}
