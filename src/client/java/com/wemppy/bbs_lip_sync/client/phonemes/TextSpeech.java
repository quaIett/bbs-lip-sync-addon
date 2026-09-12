package com.wemppy.bbs_lip_sync.client.phonemes;

import com.wemppy.bbs_lip_sync.client.model.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed text as words with made-up times, so the rest of the pipeline can't tell it from speech
 * that was heard. A word lasts as long as its letters' weights add up to at the chosen speed and
 * the next word follows right after it; punctuation adds pauses the way the old lip sync tool did
 * it: a comma a short one the mouth stays open through, a sentence end or a line break a long one
 * that closes it.
 */
public class TextSpeech
{
    /**
     * Seconds one unit of letter weight lasts at speed 1: recognized Russian speech from the test
     * recordings, the speaking time (pauses left out) over the weight of its letters. The three
     * recordings gave 0.064, 0.083 and 0.106.
     */
    public static final float SECONDS_PER_WEIGHT = 0.085F;

    public static final float MIN_SPEED = 0.5F;
    public static final float MAX_SPEED = 2F;

    /**
     * The mouth is closed on the first tick and eases into the first sound over this long. Kept
     * under two ticks, where the timeline would add an idle hold keyframe right next to it.
     */
    private static final float LEAD_IN = 1.75F / VisemeTimeline.TPS;

    /** Pause after , ; : — (seconds): shorter than the default pause that closes the mouth. */
    private static final float CLAUSE_PAUSE = 0.3F;

    /** Pause after . ! ? … and at a line break (seconds): long enough for the mouth to close. */
    private static final float SENTENCE_PAUSE = 0.6F;

    private static final String SENTENCE_END = ".!?…";
    private static final String CLAUSE_END = ",;:—–";

    /** Closing quotes and brackets don't hide the punctuation before them: «Да!» ends a sentence. */
    private static final String TRAILING = "\"'»)]}“”’";

    private record Entry(String text, float weight, float pause)
    {
        Entry pause(float pause)
        {
            return new Entry(this.text, this.weight, Math.max(this.pause, pause));
        }
    }

    /**
     * @param speed 1 is ordinary speech, 2 twice as fast; pauses keep their length
     * @return the words in order, in seconds from the start; empty when the text has no letters
     */
    public static List<Word> words(String text, LetterMap map, float speed)
    {
        List<Entry> entries = new ArrayList<>();

        for (String line : text.split("\n"))
        {
            for (String token : line.trim().split("\\s+"))
            {
                String word = clean(token);
                float pause = pauseAfter(token);
                float weight = 0F;

                for (int letter : map.letters(word))
                {
                    weight += map.weight(letter);
                }

                if (weight > 0F)
                {
                    entries.add(new Entry(word, weight, pause));
                }
                else if (!entries.isEmpty())
                {
                    /* A lone dash or ellipsis: its pause goes to the word before it */
                    entries.set(entries.size() - 1, entries.get(entries.size() - 1).pause(pause));
                }
            }

            if (!entries.isEmpty())
            {
                entries.set(entries.size() - 1, entries.get(entries.size() - 1).pause(SENTENCE_PAUSE));
            }
        }

        List<Word> words = new ArrayList<>();
        float secondsPerWeight = SECONDS_PER_WEIGHT / clampSpeed(speed);
        float cursor = LEAD_IN;

        for (Entry entry : entries)
        {
            float end = cursor + entry.weight() * secondsPerWeight;

            words.add(new Word(entry.text(), cursor, end));
            cursor = end + entry.pause();
        }

        return words;
    }

    /** The share of the text's letters (0..1) that the table has a mouth shape for. */
    public static float coverage(String text, LetterMap map)
    {
        int total = 0;
        int known = 0;

        for (String token : text.split("\\s+"))
        {
            for (int letter : map.letters(clean(token)))
            {
                total++;

                if (map.viseme(letter) != null)
                {
                    known++;
                }
            }
        }

        return total == 0 ? 0F : known / (float) total;
    }

    public static float clampSpeed(float speed)
    {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }

    /**
     * The token as a word to spell. Leading brackets go, since {@link LetterMap#letters} reads a
     * word starting with one as something the recognizer couldn't make out.
     */
    private static String clean(String token)
    {
        int start = 0;

        while (start < token.length() && !Character.isLetterOrDigit(token.charAt(start)))
        {
            start++;
        }

        return token.substring(start);
    }

    private static float pauseAfter(String token)
    {
        int end = token.length();

        while (end > 0 && TRAILING.indexOf(token.charAt(end - 1)) >= 0)
        {
            end--;
        }

        if (end == 0)
        {
            return 0F;
        }

        char last = token.charAt(end - 1);

        if (SENTENCE_END.indexOf(last) >= 0)
        {
            return SENTENCE_PAUSE;
        }

        return CLAUSE_END.indexOf(last) >= 0 ? CLAUSE_PAUSE : 0F;
    }
}
