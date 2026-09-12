package com.wemppy.bbs_lip_sync.client.bake;

import com.wemppy.bbs_lip_sync.client.phonemes.Viseme;

/**
 * The mouth shapes behind one kind of track: a keyframe value for every {@link Viseme}, found
 * through the mouth profile or by name, the missing ones borrowing a similar shape.
 */
public interface MouthRig<T>
{
    /** Whether speech can be animated at all: the closed mouth and at least one more shape. */
    boolean isUsable();

    /** The keyframe value for a shape; never null. */
    T value(Viseme viseme);

    /**
     * Poses ease into each other, so they get hold keyframes and a smooth interpolation; pictures
     * can't blend, and switch right on their keyframe.
     */
    boolean isSmooth();

    /** Where the shapes were looked for, for messages: a model id or a texture path. */
    String source();
}
