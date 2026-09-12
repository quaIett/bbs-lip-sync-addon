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
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.List;

/**
 * Writes viseme keyframes into a replay's pose channel as one undoable edit. Inside the clip's
 * range only keyframes painted in a viseme colour (this addon's from an earlier run) are replaced;
 * the user's own keyframes — expressions and the like — stay where they are.
 *
 * <p>A shape's start keyframe is a circle; its hold keyframe (same pose, near the shape's end)
 * is a diamond, so the two read differently on the timeline.</p>
 */
public class PoseBaker
{
    /** @return how many keyframes were written */
    public static int bake(Replay replay, KeyframeChannel<Pose> channel, PoseLibrary poses, List<VisemeTimeline.Key> keys, int clipTick, int clipDuration)
    {
        int[] written = {0};

        BaseValue.edit(replay.properties, IValueListener.FLAG_UNMERGEABLE, (properties) ->
        {
            float start = clipTick;
            float end = clipTick + clipDuration;
            List<Keyframe<Pose>> existing = channel.getKeyframes();

            for (int i = existing.size() - 1; i >= 0; i--)
            {
                Keyframe<Pose> keyframe = existing.get(i);
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

                int index = channel.insert(tick, poses.pose(key.viseme()).copy());
                Keyframe<Pose> keyframe = channel.get(index);
                KeyframeStyle style = keyframe.getStyle();

                keyframe.getInterpolation().setInterp(Interpolations.SINE_INOUT);
                style.setColor(Color.rgb(key.viseme().rgb));
                style.setShape(key.hold() ? KeyframeShape.DIAMOND : KeyframeShape.CIRCLE);
                style.setFilled(true);
                written[0]++;
            }
        });

        return written[0];
    }

    private static boolean occupied(KeyframeChannel<Pose> channel, float tick)
    {
        for (Keyframe<Pose> keyframe : channel.getKeyframes())
        {
            if (keyframe.getTick() == tick)
            {
                return true;
            }
        }

        return false;
    }
}
