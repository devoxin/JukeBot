package me.devoxin.jukebot.audio.filters.iir;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

// High pass filter using IIR
public class HighPass extends IIRFilter {
    public HighPass(FloatPcmAudioFilter downstream, float sampleRate, float frequency) {
        super(downstream, sampleRate, frequency);
    }

    @Override
    protected void calculateCoefficients() {
        float fracFreq = getFrequency() / getSampleRate();
        float x = (float) Math.exp(-2 * Math.PI * fracFreq);
        a = new float[] { (1 + x) / 2, -(1 + x) / 2 };
        b = new float[] { x };
    }
}
