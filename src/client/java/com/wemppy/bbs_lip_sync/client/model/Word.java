package com.wemppy.bbs_lip_sync.client.model;

/** A recognized word and when it was said, in seconds from the start of the audio that was listened to. */
public record Word(String text, float start, float end)
{
    public float length()
    {
        return this.end - this.start;
    }
}
