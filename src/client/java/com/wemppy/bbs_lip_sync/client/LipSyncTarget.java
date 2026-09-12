package com.wemppy.bbs_lip_sync.client;

import com.wemppy.bbs_lip_sync.client.bake.MouthRig;
import com.wemppy.bbs_lip_sync.client.bake.PoseLibrary;
import com.wemppy.bbs_lip_sync.client.bake.TextureLibrary;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * A timeline track lip sync can write to: the pose track of a model form, or the texture track of
 * a model, billboard or extruded form. The track decides which of the form's mouth shapes are
 * used, so a model can have both and be lip synced on either.
 */
public record LipSyncTarget(KeyframeChannel channel, Form form, boolean textures)
{
    /** The target behind a timeline row, or null if lip sync can't write there. */
    public static LipSyncTarget of(UIKeyframeSheet sheet)
    {
        if (sheet == null || sheet.header)
        {
            return null;
        }

        ModelForm posed = sheet.getPoseForm();

        if (posed != null)
        {
            return new LipSyncTarget(sheet.channel, posed, false);
        }

        if (sheet.property instanceof ValueLink && sheet.property.getId().equals("texture") && sheet.channel.getFactory() == KeyframeFactories.LINK)
        {
            Form form = FormUtils.getForm(sheet.property);

            if (form instanceof ModelForm || form instanceof BillboardForm || form instanceof ExtrudedForm)
            {
                return new LipSyncTarget(sheet.channel, form, true);
            }
        }

        return null;
    }

    /** The mouth shapes as they are set up now; null for a texture track whose form has no texture. */
    public MouthRig<?> rig()
    {
        if (!this.textures)
        {
            return PoseLibrary.of(((ModelForm) this.form).model.get());
        }

        Link closed = TextureLibrary.closedTexture(this.form);

        return closed == null ? null : TextureLibrary.of(closed);
    }

    /** The mouth shapes, or null after telling the user why there's nothing to animate with. */
    public MouthRig<?> usableRig(UIContext context)
    {
        MouthRig<?> rig = this.rig();

        if (rig == null)
        {
            LipSyncController.message(context, LipSyncKeys.NO_SHAPES_TITLE, LipSyncKeys.NO_TEXTURE_MESSAGE);

            return null;
        }

        if (!rig.isUsable())
        {
            LipSyncController.message(context, LipSyncKeys.NO_SHAPES_TITLE, LipSyncKeys.NO_SHAPES_MESSAGE.format(rig.source()));

            return null;
        }

        return rig;
    }
}
