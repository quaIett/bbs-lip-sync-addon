package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import com.wemppy.bbs_lip_sync.client.phonemes.VisemeTimeline;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import mchorse.bbs_mod.utils.keyframes.KeyframeStyle;

import java.util.List;

/**
 * Writes viseme keyframes into a replay's pose or texture channel as one undoable edit. Inside the
 * range only keyframes painted in a viseme colour (this addon's from an earlier run) are replaced;
 * the user's own keyframes, expressions and the like, stay where they are.
 *
 * <p>A shape's start keyframe is a circle; its hold keyframe (same pose, near the shape's end)
 * is a diamond, so the two read differently on the timeline. Poses ease in and out; pictures
 * can't blend, so theirs are constant.</p>
 */
public class KeyframeBaker
{
    /** @return how many keyframes were written */
    public static <T> int bake(Replay replay, KeyframeChannel<T> channel, MouthRig<T> rig, List<VisemeTimeline.Key> keys, int startTick, int duration)
    {
        int[] written = {0};

        BaseValue.edit(replay.properties, IValueListener.FLAG_UNMERGEABLE, (properties) ->
        {
            float start = startTick;
            float end = startTick + duration;
            List<Keyframe<T>> existing = channel.getKeyframes();

            for (int i = existing.size() - 1; i >= 0; i--)
            {
                Keyframe<T> keyframe = existing.get(i);
                float tick = keyframe.getTick();

                if (tick >= start && tick < end && Viseme.isOwnColor(keyframe.getStyle().getColor()))
                {
                    channel.remove(i);
                }
            }

            for (VisemeTimeline.Key key : keys)
            {
                float tick = key.tick();

                /* Never overwrite a keyframe the user placed on exactly this tick */
                while (occupied(channel, tick))
                {
                    tick += 0.25F;
                }

                if (tick >= end)
                {
                    continue;
                }

                int index = channel.insert(tick, channel.getFactory().copy(rig.value(key.viseme())));
                Keyframe<T> keyframe = channel.get(index);
                KeyframeStyle style = keyframe.getStyle();

                keyframe.getInterpolation().setInterp(rig.isSmooth() ? Interpolations.SINE_INOUT : Interpolations.CONST);
                style.setColor(Color.rgb(key.viseme().rgb));
                style.setShape(key.hold() ? KeyframeShape.DIAMOND : KeyframeShape.CIRCLE);
                style.setFilled(true);
                written[0]++;
            }
        });

        return written[0];
    }

    private static boolean occupied(KeyframeChannel<?> channel, float tick)
    {
        for (Keyframe<?> keyframe : channel.getKeyframes())
        {
            if (keyframe.getTick() == tick)
            {
                return true;
            }
        }

        return false;
    }
}
