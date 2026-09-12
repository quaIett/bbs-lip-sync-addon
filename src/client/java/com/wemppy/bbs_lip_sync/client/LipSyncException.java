package com.wemppy.bbs_lip_sync.client;

import mchorse.bbs_mod.l10n.keys.IKey;

/** A failure with a message meant for the user. */
public class LipSyncException extends Exception
{
    public final IKey message;

    public LipSyncException(IKey message, Throwable cause)
    {
        super(message.get(), cause);

        this.message = message;
    }
}
