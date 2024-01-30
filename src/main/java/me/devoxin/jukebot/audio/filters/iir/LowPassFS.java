package me.devoxin.jukebot.audio.filters.iir;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

// Four-stage low pass filter using IIR
public class LowPassFS extends IIRFilter {
    public LowPassFS(FloatPcmAudioFilter downstream, float sampleRate, float frequency) {
        super(downstream, sampleRate, Math.max(60, frequency));
    }

    @Override
    protected void calculateCoefficients() {
        float fracFreq = getFrequency() / getSampleRate();
        float x = (float) Math.exp(-14.445 * fracFreq);
        a = new float[] { (float) Math.pow(1 - x, 4) };
        b = new float[] { 4 * x, -6 * x * x, 4 * x * x * x, -x * x * x * x };
    }
}
