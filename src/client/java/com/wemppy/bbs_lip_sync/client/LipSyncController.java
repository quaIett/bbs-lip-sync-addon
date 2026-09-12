package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.client.bake.PoseLibrary;
import com.wemppy.bbs_lip_sync.client.model.Language;
import com.wemppy.bbs_lip_sync.client.model.Languages;
import com.wemppy.bbs_lip_sync.client.model.ModelStore;
import com.wemppy.bbs_lip_sync.client.ui.AudioClipPicker;
import com.wemppy.bbs_lip_sync.client.ui.UIModelDownloadOverlayPanel;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;

/**
 * What happens after "Generate lip sync" is clicked on a pose track: check the rig has mouth
 * poses, pick the audio clip, make sure the language's model is on disk, then run a
 * {@link LipSyncJob}.
 */
public class LipSyncController
{
    @SuppressWarnings("unchecked")
    public static void generate(UIReplaysEditor editor, UIKeyframeSheet sheet)
    {
        UIFilmPanel panel = editor.getParent(UIFilmPanel.class);
        Replay replay = editor.getReplay();
        ModelForm form = sheet.getPoseForm();
        UIContext context = editor.getContext();

        if (panel == null || replay == null || form == null || context == null || LipSyncJob.isBusy())
        {
            return;
        }

        Film film = panel.getData();
        String modelId = form.model.get();
        PoseLibrary poses = PoseLibrary.of(modelId);

        if (!poses.isUsable())
        {
            message(context, LipSyncKeys.NO_POSES_TITLE, LipSyncKeys.NO_POSES_MESSAGE.format(modelId));

            return;
        }

        KeyframeChannel<Pose> channel = (KeyframeChannel<Pose>) sheet.channel;

        AudioClipPicker.pick(film, panel.getCursor(), context, (clip) ->
        {
            Runnable start = () -> start(editor, panel, replay, channel, poses, clip);
            Language language = Languages.current();

            if (language != null && ModelStore.isReady(language.model(LipSyncSettings.largeModel())))
            {
                start.run();
            }
            else
            {
                ModelStore.onceReady(start);
                UIOverlay.addOverlay(context, new UIModelDownloadOverlayPanel(language));
            }
        });
    }

    /** Runs the job for whatever language is current now — it may have been changed in the model dialog. */
    private static void start(UIReplaysEditor editor, UIFilmPanel panel, Replay replay, KeyframeChannel<Pose> channel, PoseLibrary poses, AudioClip clip)
    {
        Language language = Languages.current();

        if (LipSyncJob.isBusy() || clip.audio.get() == null || language == null)
        {
            return;
        }

        Language.Model model = language.model(LipSyncSettings.largeModel());

        if (!ModelStore.isReady(model))
        {
            return;
        }

        Runnable onBaked = () ->
        {
            panel.getUndoHandler().getUndoManager().markLastUndoNoMerging();
            editor.updateChannelsList();
        };

        new LipSyncJob(
            replay, channel, poses, language, model,
            clip.audio.get(), clip.offset.get(), clip.tick.get(), clip.duration.get(),
            onBaked, (error) -> message(editor.getContext(), LipSyncKeys.ERROR_TITLE, error)
        ).start();
    }

    public static void message(UIContext context, IKey title, IKey message)
    {
        if (context != null)
        {
            UIOverlay.addOverlay(context, new UIMessageOverlayPanel(title, message));
        }
    }
}
