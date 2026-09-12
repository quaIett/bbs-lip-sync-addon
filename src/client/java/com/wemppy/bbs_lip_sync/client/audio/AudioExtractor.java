package com.wemppy.bbs_lip_sync.client.audio;

import com.wemppy.bbs_lip_sync.client.LipSyncException;
import com.wemppy.bbs_lip_sync.client.LipSyncKeys;
import com.wemppy.bbs_lip_sync.client.model.SpeechRecognizer;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.resources.Link;

/**
 * Reads the audio behind a film clip and hands back the stretch the clip plays, as 16 kHz mono
 * samples in [-1, 1] — what the speech recognizer expects.
 *
 * <p>A film tick inside the clip maps to {@code (tick - clip.tick + clip.offset) / 20} seconds
 * into the file, so sample {@code i} of the result is {@code i / 16000} seconds after the clip
 * starts. Parts of the clip before the file's start or after its end are silence.</p>
 */
public class AudioExtractor
{
    /**
     * @param offsetTicks   the clip's {@code offset}: where in the file the clip starts, in ticks
     * @param durationTicks the clip's length in ticks
     */
    public static float[] extract(Link link, int offsetTicks, int durationTicks) throws LipSyncException
    {
        Wave wave;

        try
        {
            wave = AudioReader.read(BBSMod.getProvider(), link);
        }
        catch (Exception e)
        {
            throw new LipSyncException(LipSyncKeys.ERROR_AUDIO_UNREADABLE.format(String.valueOf(link)), e);
        }

        if (wave == null || wave.data == null)
        {
            throw new LipSyncException(LipSyncKeys.ERROR_AUDIO_UNREADABLE.format(String.valueOf(link)), null);
        }

        if (wave.getBytesPerSample() != 2)
        {
            wave = wave.convertTo16();
        }

        if (wave.getBytesPerSample() != 2)
        {
            throw new LipSyncException(LipSyncKeys.ERROR_AUDIO_UNREADABLE.format(String.valueOf(link)), null);
        }

        int channels = Math.max(1, wave.numChannels);
        int sourceRate = wave.sampleRate;
        int frames = wave.data.length / (2 * channels);
        float[] mono = new float[frames];

        for (int f = 0; f < frames; f++)
        {
            int sum = 0;

            for (int c = 0; c < channels; c++)
            {
                int i = (f * channels + c) * 2;

                sum += (short) ((wave.data[i] & 0xff) | (wave.data[i + 1] << 8));
            }

            mono[f] = sum / (float) channels / 32768F;
        }

        int outRate = SpeechRecognizer.SAMPLE_RATE;

        if (sourceRate > outRate)
        {
            mono = lowPass(mono, 0.45F * outRate / sourceRate);
        }

        double from = offsetTicks / 20D;
        double seconds = durationTicks / 20D;
        int outLength = (int) Math.round(seconds * outRate);
        float[] out = new float[outLength];

        for (int i = 0; i < outLength; i++)
        {
            double t = from + i / (double) outRate;
            double position = t * sourceRate;

            if (position < 0 || position >= frames - 1)
            {
                continue;
            }

            int index = (int) position;
            float fraction = (float) (position - index);

            out[i] = mono[index] + (mono[index + 1] - mono[index]) * fraction;
        }

        return out;
    }

    /**
     * Windowed-sinc low-pass before downsampling, so the frequencies above the new Nyquist fold
     * into noise instead of into the speech band.
     *
     * @param cutoff in cycles per source sample
     */
    private static float[] lowPass(float[] in, float cutoff)
    {
        int taps = 33;
        int middle = taps / 2;
        float[] kernel = new float[taps];
        double sum = 0;

        for (int i = 0; i < taps; i++)
        {
            int n = i - middle;
            double sinc = n == 0 ? 2 * cutoff : Math.sin(2 * Math.PI * cutoff * n) / (Math.PI * n);
            double hamming = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (taps - 1));

            kernel[i] = (float) (sinc * hamming);
            sum += kernel[i];
        }

        for (int i = 0; i < taps; i++)
        {
            kernel[i] /= (float) sum;
        }

        float[] out = new float[in.length];

        for (int i = 0; i < in.length; i++)
        {
            float acc = 0;

            for (int j = 0; j < taps; j++)
            {
                int index = i + j - middle;

                if (index >= 0 && index < in.length)
                {
                    acc += kernel[j] * in[index];
                }
            }

            out[i] = acc;
        }

        return out;
    }
}
