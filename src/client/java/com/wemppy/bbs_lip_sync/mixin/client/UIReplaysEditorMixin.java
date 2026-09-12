package com.wemppy.bbs_lip_sync.mixin.client;

import com.wemppy.bbs_lip_sync.client.ui.LipSyncTimeline;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The replay keyframe editor is destroyed and rebuilt by {@code updateChannelsList()} on every
 * replay/category switch, so the context menu entry and the progress painter have to be attached
 * again after each rebuild. BBS has no event for that; this is the only hook.
 */
@Mixin(value = UIReplaysEditor.class, remap = false)
public class UIReplaysEditorMixin
{
    @Inject(method = "updateChannelsList", at = @At("TAIL"), remap = false)
    private void bbs_lip_sync$afterUpdateChannelsList(CallbackInfo info)
    {
        LipSyncTimeline.attach((UIReplaysEditor) (Object) this);
    }
}
