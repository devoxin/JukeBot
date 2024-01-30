package me.devoxin.jukebot.audio.filters.iir;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

// Single-pass low pass filter using IIR
public class LowPassSP extends IIRFilter {
    public LowPassSP(FloatPcmAudioFilter downstream, float sampleRate, float frequency) {
        super(downstream, sampleRate, frequency);
    }

    @Override
    protected void calculateCoefficients() {
        float fracFreq = getFrequency() / getSampleRate();
        float x = (float) Math.exp(-2 * Math.PI * fracFreq);
        a = new float[] { 1 - x };
        b = new float[] { x };
    }
}
