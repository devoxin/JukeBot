package me.devoxin.jukebot.audio.filters.unusable;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import me.devoxin.jukebot.audio.filters.iir.IIRFilter;

public class BandPassFilter extends IIRFilter {
    private float bandWidth;

    public BandPassFilter(FloatPcmAudioFilter downstream, float sampleRate, float frequency, float bandWidth) {
        super(downstream, frequency, sampleRate);
        setBandwidth(bandWidth);
    }

    private void setBandwidth(float bandWidth) {
        this.bandWidth = bandWidth / getSampleRate();
        calculateCoefficients();
    }

    // TODO: This needs work... only 2500 frequency seems to work??
    @Override
    protected void calculateCoefficients() {
        float R = 1 - 3 * bandWidth;
        float fracFreq = getFrequency() / getSampleRate();
        float T = 2 * (float) Math.cos(2 * Math.PI * fracFreq);
        float K = (1 - R * T + R * R) / (2 - T);
        a = new float[] { 1 - K, (K - R) * T, R * R - K };
        b = new float[] { R * T, -R * R };
    }
}
