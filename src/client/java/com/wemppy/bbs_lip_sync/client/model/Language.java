package com.wemppy.bbs_lip_sync.client.model;

import java.util.List;

/**
 * A language lip sync can listen to: which Vosk models hear it and which letter table turns its
 * spelling into mouth shapes. Comes from the bundled {@code languages.json}.
 */
public class Language
{
    /** One downloadable Vosk model: the folder name it unpacks to, where it comes from, roughly how big it is. */
    public record Model(String name, String url, int megabytes)
    {}

    /** Short id, e.g. {@code ru}; also the settings value. */
    public final String code;

    /** Native name, e.g. Русский. */
    public final String name;

    /** Minecraft language prefixes (the part before {@code _}) this language is picked for automatically. */
    public final List<String> game;

    /** Id of the letter table in {@code letters/}. */
    public final String letters;

    public final Model small;
    public final Model large;

    public Language(String code, String name, List<String> game, String letters, Model small, Model large)
    {
        this.code = code;
        this.name = name;
        this.game = game;
        this.letters = letters;
        this.small = small;
        this.large = large;
    }

    /** The model for the requested size, or whichever one exists. */
    public Model model(boolean large)
    {
        if (large && this.large != null)
        {
            return this.large;
        }

        return this.small != null ? this.small : this.large;
    }

    public boolean hasLarge()
    {
        return this.large != null;
    }

    /** How the language is listed: {@code Русский (ru)}. */
    public String label()
    {
        return this.name + " (" + this.code + ")";
    }

    @Override
    public String toString()
    {
        return this.code;
    }
}
