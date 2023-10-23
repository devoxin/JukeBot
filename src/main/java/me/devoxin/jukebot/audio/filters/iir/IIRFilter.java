package me.devoxin.jukebot.audio.filters.iir;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

public abstract class IIRFilter implements FloatPcmAudioFilter {
    protected float[] b;
    protected float[] a;

    protected float[] in;
    protected float[] out;

    private final FloatPcmAudioFilter downstream;
    private final float frequency;
    private final float sampleRate;

    public IIRFilter(FloatPcmAudioFilter downstream, float sampleRate, float frequency) {
        this.downstream = downstream;
        this.frequency = frequency;
        this.sampleRate = sampleRate;
        calculateCoefficients();
        in = new float[a.length];
        out = new float[b.length];
    }

    protected abstract void calculateCoefficients();

    public float getFrequency() {
        return this.frequency;
    }

    public float getSampleRate() {
        return this.sampleRate;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        for (int i = offset; i < offset + length; i++) {
            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = input[0][i];

            float y = 0;
            for (int j = 0; j < a.length; j++) {
                y += a[j] * in[j];
            }

            for (int j = 0; j < b.length; j++) {
                y += b[j] * out[j];
            }

            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;

            for (int channel = 0; channel < input.length; channel++) {
                input[channel][i] = y;
            }
        }

        downstream.process(input, offset, length);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {

    }

    @Override
    public void flush() {
        in = new float[a.length];
        out = new float[b.length];
        calculateCoefficients();
    }

    @Override
    public void close() {

    }
}
