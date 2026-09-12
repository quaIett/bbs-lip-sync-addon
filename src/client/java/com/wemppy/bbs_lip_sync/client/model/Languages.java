package com.wemppy.bbs_lip_sync.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wemppy.bbs_lip_sync.client.Bundled;
import com.wemppy.bbs_lip_sync.client.LipSyncSettings;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The languages lip sync knows, from the bundled {@code languages.json}. Which one is used comes
 * from the settings, and by default from the language Minecraft runs in.
 */
public class Languages
{
    /** What is used when nothing else fits, e.g. the game runs in a language with no model. */
    public static final String FALLBACK = "en";

    private static final String BASE_URL = "https://alphacephei.com/vosk/models/";

    private static List<Language> all;

    public static synchronized List<Language> all()
    {
        if (all == null)
        {
            try
            {
                all = Collections.unmodifiableList(fromJson(Bundled.read("languages.json")));
            }
            catch (Exception e)
            {
                e.printStackTrace();

                all = Collections.emptyList();
            }
        }

        return all;
    }

    public static Language byCode(String code)
    {
        if (code == null)
        {
            return null;
        }

        for (Language language : all())
        {
            if (language.code.equalsIgnoreCase(code.trim()))
            {
                return language;
            }
        }

        return null;
    }

    /** The language chosen in the settings, else the game's language, else {@link #FALLBACK}. */
    public static Language current()
    {
        Language language = byCode(LipSyncSettings.language());

        if (language == null)
        {
            language = detect();
        }

        if (language == null)
        {
            language = byCode(FALLBACK);
        }

        if (language == null && !all().isEmpty())
        {
            language = all().get(0);
        }

        return language;
    }

    /** The language matching Minecraft's, e.g. {@code ru_ru} → {@code ru}; null if none does. */
    public static Language detect()
    {
        String game;

        try
        {
            MinecraftClient client = MinecraftClient.getInstance();

            game = client == null || client.getLanguageManager() == null ? null : client.getLanguageManager().getLanguage();
        }
        catch (Throwable e)
        {
            game = null;
        }

        if (game == null)
        {
            return null;
        }

        int underscore = game.indexOf('_');
        String prefix = (underscore > 0 ? game.substring(0, underscore) : game).toLowerCase();

        for (Language language : all())
        {
            if (language.game.contains(prefix) || language.code.equals(prefix))
            {
                return language;
            }
        }

        return null;
    }

    public static List<Language> fromJson(String json)
    {
        List<Language> languages = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        for (JsonElement element : array)
        {
            JsonObject object = element.getAsJsonObject();
            String code = object.get("code").getAsString();
            String name = object.get("name").getAsString();
            List<String> game = new ArrayList<>();

            if (object.has("game"))
            {
                for (JsonElement g : object.getAsJsonArray("game"))
                {
                    game.add(g.getAsString().toLowerCase());
                }
            }

            String letters = object.has("letters") ? object.get("letters").getAsString() : code;
            Language.Model small = model(object.get("small"));
            Language.Model large = model(object.get("large"));

            if (small == null && large == null)
            {
                continue;
            }

            languages.add(new Language(code, name, game, letters, small, large));
        }

        return languages;
    }

    private static Language.Model model(JsonElement element)
    {
        if (element == null || !element.isJsonObject())
        {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        String name = object.get("name").getAsString();
        String url = object.has("url") ? object.get("url").getAsString() : BASE_URL + name + ".zip";
        int megabytes = object.has("mb") ? object.get("mb").getAsInt() : 0;

        return new Language.Model(name, url, megabytes);
    }
}
