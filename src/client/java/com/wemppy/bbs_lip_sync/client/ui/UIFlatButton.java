package com.wemppy.bbs_lip_sync.client.ui;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

/**
 * A secondary button. A plain BBS button is painted in the accent colour, so next to one of
 * those the main action of a dialog doesn't stand out; this one sits in the surface instead and
 * leaves the accent to the button that matters. Its colours follow the theme every frame.
 */
public class UIFlatButton extends UIButton
{
    public UIFlatButton(IKey label, Consumer<UIButton> callback)
    {
        super(label, callback);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        int fill = this.hover ? BBSSettings.baseSurface() : BBSSettings.deepSurface();
        boolean light = BBSSettings.lightSurfaces();
        int text = light ? Colors.DARKEST_GRAY : (this.hover ? Colors.WHITE : Colors.LIGHTEST_GRAY);

        context.batcher.surfaceBox(this.area.x, this.area.y, this.area.ex(), this.area.ey(), fill | Colors.A100, true, false);

        FontRenderer font = context.batcher.getFont();
        String label = font.limitToWidth(this.label.get(), this.area.w - 4);

        context.batcher.text(label, this.area.mx(font.getWidth(label)), this.area.my(font.getHeight()), text);

        this.renderLockedArea(context);
    }
}
