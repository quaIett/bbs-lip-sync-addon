package com.wemppy.bbs_lip_sync.client.ui;

import com.wemppy.bbs_lip_sync.client.LipSyncJob;
import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

/** Draws the current job's stage and progress over its track row, spanning the audio clip. */
public class LipSyncProgressPainter
{
    public static void render(UIContext context, UIKeyframeEditor editor)
    {
        LipSyncJob job = LipSyncJob.current();

        if (job == null)
        {
            return;
        }

        UIKeyframeDopeSheet dope = editor.view.getDopeSheet();

        if (editor.view.getGraph() != dope)
        {
            return;
        }

        UIKeyframeSheet sheet = null;

        for (UIKeyframeSheet candidate : dope.getSheets())
        {
            if (candidate.channel == job.channel)
            {
                sheet = candidate;

                break;
            }
        }

        if (sheet == null)
        {
            return;
        }

        Area area = editor.view.graphArea;
        int y = dope.getDopeSheetY(sheet);
        int h = dope.getTrackHeight(sheet);

        if (y + h <= area.y || y >= area.ey())
        {
            return;
        }

        int x1 = Math.max(area.x, editor.view.toGraphX(job.clipTick));
        int x2 = Math.min(area.ex(), editor.view.toGraphX(job.clipTick + job.clipDuration));

        if (x2 - x1 < 2)
        {
            return;
        }

        context.batcher.clip(area, context);
        context.batcher.box(x1, y, x2, y + h, Colors.A75);

        float fraction = fraction(job);

        if (fraction > 0)
        {
            context.batcher.box(x1, y, x1 + (x2 - x1) * fraction, y + h, BBSSettings.primaryColor(Colors.A75));
        }

        int color = job.stage() == LipSyncJob.Stage.FAILED ? Colors.RED : Colors.WHITE;
        String label = label(job);
        int textY = y + (h - context.batcher.getFont().getHeight()) / 2;

        context.batcher.textShadow(label, x1 + 4, textY, color | Colors.A100);
        context.batcher.unclip(context);
    }

    private static float fraction(LipSyncJob job)
    {
        switch (job.stage())
        {
            case LOADING: return 0F;
            case RECOGNIZING: return job.progress() / 100F;
            case BAKING: return 1F;
            case DONE: return 1F;
            default: return 0F;
        }
    }

    private static String label(LipSyncJob job)
    {
        switch (job.stage())
        {
            case LOADING: return LipSyncKeys.PROGRESS_LOADING.get();
            case RECOGNIZING: return LipSyncKeys.PROGRESS_RECOGNIZING.format(job.progress()).get();
            case BAKING: return LipSyncKeys.PROGRESS_BAKING.get();
            case DONE: return LipSyncKeys.PROGRESS_DONE.format(job.written()).get();
            case NO_SPEECH: return LipSyncKeys.PROGRESS_NO_SPEECH.get();
            case CANCELLED: return LipSyncKeys.PROGRESS_CANCELLED.get();
            default: return LipSyncKeys.ERROR_TITLE.get();
        }
    }
}
