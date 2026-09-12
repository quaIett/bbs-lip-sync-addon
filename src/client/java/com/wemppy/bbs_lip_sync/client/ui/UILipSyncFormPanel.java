package com.wemppy.bbs_lip_sync.client.ui;

import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import com.wemppy.bbs_lip_sync.client.bake.MouthProfile;
import com.wemppy.bbs_lip_sync.client.bake.PoseLibrary;
import com.wemppy.bbs_lip_sync.client.bake.TextureLibrary;
import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The «Lip Sync» tab of the form editor: which pose or picture stands for each mouth shape. Each
 * shape is a row in the timeline's row style, with its keyframe colour on the edge, its main
 * letter and the other letters that make it, and what stands for it. A click picks the row, shows
 * that shape on the form and opens the choice; a right click forgets the choice. Every change
 * goes to {@code lipsync.json} at once (see {@link MouthProfile}), like the other tabs, which
 * have no save button either.
 *
 * <p>A model form switches between its two maps, poses and textures. Both are kept: the track a
 * generation runs on decides which one is used. Billboards and extruded forms only have
 * textures.</p>
 *
 * <p>The picked row is shown on the form only while this tab is open, see {@link MouthPreview}.</p>
 */
public class UILipSyncFormPanel<T extends Form> extends UIFormPanel<T>
{
    private static final int MODE_POSES = 0;
    private static final int MODE_TEXTURES = 1;

    /** Two lines: the letters, then what stands for the shape. */
    private static final int ROW_HEIGHT = 28;

    /** The map last shown for a form, so the tab doesn't flip back each time the editor rebuilds it. */
    private static final Map<Form, Integer> MODES = new WeakHashMap<>();

    /** The row last picked for a form, for the same reason. */
    private static final Map<Form, Viseme> PICKED = new WeakHashMap<>();

    private final ModeSwitch mode;
    private final UIText status;

    private PoseLibrary poses;
    private TextureLibrary textures;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <F extends UIForm> F attach(F editor)
    {
        editor.registerPanel(new UILipSyncFormPanel(editor), LipSyncKeys.FORM_TITLE, Icons.VOICE);

        return editor;
    }

    public UILipSyncFormPanel(UIForm editor)
    {
        super(editor);

        this.mode = new ModeSwitch();
        this.status = new UIText().color(Colors.GRAY, false);

        this.options.add(this.mode);

        for (Viseme viseme : Viseme.values())
        {
            this.options.add(new Row(viseme));
        }

        this.options.add(this.status);
    }

    @Override
    public void startEdit(T form)
    {
        MouthPreview.clear(this);

        super.startEdit(form);

        boolean model = form instanceof ModelForm;
        Integer remembered = MODES.get(form);

        this.mode.setVisible(model);
        this.mode.value = !model ? MODE_TEXTURES : remembered != null ? remembered : this.defaultMode((ModelForm) form);
        this.refresh();
    }

    @Override
    public void finishEdit()
    {
        MouthPreview.clear(this);

        super.finishEdit();
    }

    @Override
    protected void onRemove(UIElement parent)
    {
        MouthPreview.clear(this);

        super.onRemove(parent);
    }

    @Override
    public void render(UIContext context)
    {
        Viseme picked = this.form == null ? null : PICKED.get(this.form);

        if (picked != null && !MouthPreview.isShownBy(this))
        {
            this.preview(picked);
        }

        MouthPreview.confirm(this);

        super.render(context);
    }

    /** Poses, unless only the textures are set up. */
    private int defaultMode(ModelForm form)
    {
        if (PoseLibrary.of(form.model.get()).isUsable())
        {
            return MODE_POSES;
        }

        Link closed = TextureLibrary.closedTexture(form);

        return closed != null && TextureLibrary.of(closed).isUsable() ? MODE_TEXTURES : MODE_POSES;
    }

    private void switchMode(int mode)
    {
        this.mode.value = mode;
        MODES.put(this.form, mode);
        this.refresh();
    }

    /**
     * Reads the shapes again and says what's wrong, if anything. The preview is taken down, and
     * the next frame puts it up again from what was just read.
     */
    private void refresh()
    {
        MouthPreview.clear(this);

        this.poses = null;
        this.textures = null;

        IKey status = null;

        if (this.mode.value == MODE_TEXTURES)
        {
            Link closed = TextureLibrary.closedTexture(this.form);

            this.textures = closed == null ? null : TextureLibrary.of(closed);
            status = closed == null ? LipSyncKeys.FORM_NO_TEXTURE : this.textures.isUsable() ? null : LipSyncKeys.FORM_NOT_ENOUGH;
        }
        else
        {
            this.poses = PoseLibrary.of(((ModelForm) this.form).model.get());
            status = this.poses.all().isEmpty() ? LipSyncKeys.FORM_NO_POSES : this.poses.isUsable() ? null : LipSyncKeys.FORM_NOT_ENOUGH;
        }

        this.status.setVisible(status != null);
        this.status.text(status == null ? IKey.EMPTY : status);

        if (this.hasParent())
        {
            this.resize();
        }
    }

    private void pick(Viseme viseme)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        PICKED.put(this.form, viseme);
        this.preview(viseme);

        if (this.textures != null)
        {
            Link closed = this.textures.closed();
            Link current = this.textures.texture(viseme);

            UITexturePicker.open(context, current == null ? closed : current, (link) ->
            {
                if (link != null && this.textures != null && closed.equals(this.textures.closed()))
                {
                    MouthProfile.setTexture(closed, viseme, viseme == Viseme.IDLE && link.equals(closed) ? null : link);
                    this.refresh();
                }
            });
        }
        else if (this.poses != null && !this.poses.all().isEmpty())
        {
            String model = this.poses.source();
            UIStringOverlayPanel[] panel = new UIStringOverlayPanel[1];

            panel[0] = new UIStringOverlayPanel(LipSyncKeys.FORM_PICK_POSE, true, this.poses.all(), (name) ->
            {
                MouthProfile.setPose(model, viseme, name);
                panel[0].close();
                this.refresh();
            });

            String current = this.poses.name(viseme);

            if (current != null)
            {
                panel[0].set(current);
            }

            UIOverlay.addOverlay(context, panel[0], 240, 0.6F);
        }
    }

    private void reset(Viseme viseme)
    {
        if (this.textures != null)
        {
            MouthProfile.setTexture(this.textures.closed(), viseme, null);
        }
        else if (this.poses != null)
        {
            MouthProfile.setPose(this.poses.source(), viseme, null);
        }

        PICKED.put(this.form, viseme);
        this.refresh();
    }

    private void preview(Viseme viseme)
    {
        if (this.textures != null && this.form.getBasic("texture") instanceof ValueLink texture)
        {
            MouthPreview.show(this, texture, this.textures.value(viseme));
        }
        else if (this.poses != null && this.form instanceof ModelForm model)
        {
            MouthPreview.show(this, model.pose, this.poses.value(viseme).copy());
        }
    }

    /**
     * «Poses | Textures» as two halves side by side, marked the way the icon strips mark the
     * active cell.
     */
    private class ModeSwitch extends UIElement
    {
        private final Icon[] icons = {Icons.POSE, Icons.MATERIAL};
        private final IKey[] labels = {LipSyncKeys.FORM_POSES, LipSyncKeys.FORM_TEXTURES};

        private int value;

        ModeSwitch()
        {
            this.h(UIConstants.CONTROL_HEIGHT);
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && this.area.isInside(context))
            {
                int index = context.mouseX < this.area.mx() ? MODE_POSES : MODE_TEXTURES;

                if (index != this.value)
                {
                    UIUtils.playClick();
                    switchMode(index);
                }

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            FontRenderer font = context.batcher.getFont();
            boolean hover = this.area.isInside(context);
            int half = this.area.w / 2;

            this.area.render(context.batcher, BBSSettings.deepSurface());

            for (int i = 0; i < 2; i++)
            {
                int x = this.area.x + i * half;
                int w = i == 0 ? half : this.area.w - half;
                boolean active = i == this.value;
                boolean cellHover = hover && (context.mouseX < this.area.mx()) == (i == 0);

                Area.SHARED.set(x, this.area.y, w, this.area.h);

                if (active)
                {
                    context.batcher.highlight(Area.SHARED, Direction.BOTTOM);
                }
                else if (cellHover)
                {
                    RowStyle.hover(context.batcher, x, this.area.y, w, this.area.h, 0);
                }

                if (cellHover && !active)
                {
                    context.requestCursor(GLFW.GLFW_HAND_CURSOR);
                }

                int color = active ? Colors.WHITE : cellHover ? Colors.LIGHTEST_GRAY : Colors.setA(Colors.WHITE, 0.35F);
                String label = this.labels[i].get();
                int room = w - 20;
                boolean withLabel = font.getWidth(label) <= room - 2;
                int contentW = 16 + (withLabel ? font.getWidth(label) + 2 : 0);
                int cx = x + (w - contentW) / 2;

                context.batcher.icon(this.icons[i], color, cx, this.area.my(), 0F, 0.5F);

                if (withLabel)
                {
                    context.batcher.text(label, cx + 18, this.area.my(font.getHeight()), color);
                }
            }

            super.render(context);
        }
    }

    /**
     * A shape as a row: its keyframe colour on the edge, the main letter bright and the other
     * letters quiet on the first line, what stands for it on the second (a picture also gets a
     * thumbnail). Hover and pick are the timeline rows' marks.
     */
    private class Row extends UIElement
    {
        private final Viseme viseme;
        private final IKey letter;
        private final IKey more;

        Row(Viseme viseme)
        {
            this.viseme = viseme;
            this.letter = LipSyncKeys.shape(viseme);
            this.more = LipSyncKeys.shapeMore(viseme);

            this.h(ROW_HEIGHT);
            this.context((menu) -> menu.action(Icons.REFRESH, LipSyncKeys.FORM_RESET, () -> reset(viseme)));
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 0 && this.area.isInside(context))
            {
                UIUtils.playClick();
                pick(this.viseme);

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            FontRenderer font = context.batcher.getFont();
            boolean hover = this.area.isInside(context);
            boolean picked = form != null && PICKED.get(form) == this.viseme && MouthPreview.isShownBy(UILipSyncFormPanel.this);
            boolean lit = hover || picked;

            if (hover)
            {
                context.requestCursor(GLFW.GLFW_HAND_CURSOR);
            }

            RowStyle.row(context.batcher, this.area.x, this.area.y, this.area.w, this.area.h, this.viseme.rgb, false, hover, picked);

            int x = this.area.x + RowStyle.STRIPE + 4;
            String name = null;

            if (textures != null)
            {
                Link link = textures.texture(this.viseme);
                int size = this.area.h - 6;

                if (link != null)
                {
                    this.thumbnail(context, link, x, this.area.y + 3, size);
                    name = MouthProfile.fileName(link);
                }

                x += size + 4;
            }
            else if (poses != null)
            {
                name = poses.name(this.viseme);
            }

            int right = this.area.ex() - 4;
            int top = this.area.y + 3;
            String letter = font.limitToWidth(this.letter.get(), right - x);

            context.batcher.text(letter, x, top, RowStyle.textColor(lit));

            String more = this.more.get();
            int moreX = x + font.getWidth(letter) + 4;

            if (!more.isEmpty() && moreX < right)
            {
                context.batcher.text(font.limitToWidth(more, right - moreX), moreX, top, RowStyle.textColor(lit, Colors.GRAY));
            }

            String value = font.limitToWidth(name == null ? "—" : name, right - x);

            context.batcher.text(value, x, top + 12, RowStyle.textColor(lit, name == null ? Colors.GRAY : Colors.LIGHTER_GRAY));

            super.render(context);
        }

        private void thumbnail(UIContext context, Link link, int x, int y, int size)
        {
            Texture texture = context.render.getTextures().getTexture(link);

            if (texture == null || texture.width <= 0 || texture.height <= 0)
            {
                return;
            }

            float scale = Math.min(size / (float) texture.width, size / (float) texture.height);
            float w = texture.width * scale;
            float h = texture.height * scale;

            context.batcher.fullTexturedBox(texture, x + (size - w) / 2F, y + (size - h) / 2F, w, h);
        }
    }
}
