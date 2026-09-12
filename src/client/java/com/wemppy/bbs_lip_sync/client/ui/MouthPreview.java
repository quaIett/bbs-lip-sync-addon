package com.wemppy.bbs_lip_sync.client.ui;

import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

/**
 * The mouth shape the «Lip Sync» tab shows on a form, through the value's runtime slot (the one
 * film keyframes drive and nothing saves). It stays up only while the tab that put it there is
 * being drawn: the tab confirms it every frame, and before any form is drawn a preview nobody has
 * confirmed for a moment is taken down.
 *
 * <p>The tab can't simply clean up after itself. When the form editor rebuilds (a click on a body
 * part or a bone does it), the old tab is dropped without being told, and its preview would stay
 * on the form for good.</p>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class MouthPreview
{
    /** How long a preview survives without its tab being drawn (ms). */
    private static final long TIMEOUT = 250;

    private static BaseValueBasic value;
    private static Object owner;
    private static long confirmed;

    static
    {
        FormRenderEvents.BEFORE.register((form, context) -> expire());
    }

    static void show(Object owner, BaseValueBasic value, Object shape)
    {
        clear();

        value.setRuntimeValue(shape);

        MouthPreview.value = value;
        MouthPreview.owner = owner;
        MouthPreview.confirmed = System.currentTimeMillis();
    }

    static boolean isShownBy(Object owner)
    {
        return value != null && MouthPreview.owner == owner;
    }

    /** Called by the tab every frame it is drawn. */
    static void confirm(Object owner)
    {
        if (isShownBy(owner))
        {
            confirmed = System.currentTimeMillis();
        }
    }

    static void clear(Object owner)
    {
        if (isShownBy(owner))
        {
            clear();
        }
    }

    static void clear()
    {
        if (value != null)
        {
            value.setRuntimeValue(null);
        }

        value = null;
        owner = null;
    }

    private static void expire()
    {
        if (value != null && System.currentTimeMillis() - confirmed > TIMEOUT)
        {
            clear();
        }
    }
}
