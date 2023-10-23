package me.devoxin.jukebot.audio.filters;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

public abstract class ShelfFilter implements FloatPcmAudioFilter {
    private final FloatPcmAudioFilter downstream;
    protected final int sampleRate;
    private double boostFactor;

    protected final double[] b = new double[3];
    protected final double[] a = new double[3];
    protected final float[][] x1;
    protected final float[][] y1;

    public ShelfFilter(FloatPcmAudioFilter downstream, int sampleRate, int channelCount, int cutoffFrequency) {
        this(downstream, sampleRate, channelCount, cutoffFrequency, 1.0f);
    }

    public ShelfFilter(FloatPcmAudioFilter downstream, int sampleRate, int channelCount, int cutoffFrequency, double boostFactor) {
        this.downstream = downstream;
        this.sampleRate = sampleRate;
        this.boostFactor = boostFactor;

        this.x1 = new float[channelCount][2];
        this.y1 = new float[channelCount][2];

        setCutoffFrequency(cutoffFrequency);
    }

    protected abstract void setCutoffFrequency(int cutoffFrequency);

    public void setBoostFactor(double boostFactor) {
        this.boostFactor = boostFactor;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        for (int channel = 0; channel < input.length; channel++) {
            for (int i = offset; i < offset + length; i++) {
                float x0 = input[channel][i];
                float y0 = (float) (b[0] * x0 + b[1] * x1[channel][0] + b[2] * x1[channel][1] - a[1] * y1[channel][0] - a[2] * y1[channel][1]);
                x1[channel][1] = x1[channel][0];
                x1[channel][0] = x0;
                y1[channel][1] = y1[channel][0];
                y1[channel][0] = y0;
                input[channel][i] = (float) (y0 * boostFactor);
            }
        }

        downstream.process(input, offset, length);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {

    }

    @Override
    public void flush() throws InterruptedException {
        for (int channel = 0; channel < x1.length; channel++) {
            x1[channel][0] = 0.0f;
            x1[channel][1] = 0.0f;
        }

        for (int channel = 0; channel < y1.length; channel++) {
            y1[channel][0] = 0.0f;
            y1[channel][1] = 0.0f;
        }
    }

    @Override
    public void close() {

    }
}
