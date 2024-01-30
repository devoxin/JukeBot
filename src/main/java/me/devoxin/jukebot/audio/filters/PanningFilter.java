package me.devoxin.jukebot.audio.filters;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

// Experimental
public class PanningFilter implements FloatPcmAudioFilter {
    private final FloatPcmAudioFilter downstream;

    public PanningFilter(FloatPcmAudioFilter downstream) {
        this.downstream = downstream;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        for (int i = offset; i < offset + length; i++) {
            float pan = calculatePanValue(i - offset, length);

            for (int channel = 0; channel < input.length; channel++) {
                input[channel][i] *= (channel == 0) ? 1.0f - pan : pan;
            }
        }

        downstream.process(input, offset, length);
    }

    private float calculatePanValue(int index, int length) {
        // Calculate the pan value using a sine function for smoother transition
        float normalizedIndex = (float) index / (float) length;
        return 0.5f + 0.5f * (float) Math.sin(normalizedIndex * Math.PI);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {
        downstream.seekPerformed(requestedTime, providedTime);
    }

    @Override
    public void flush() throws InterruptedException {
        downstream.flush();
    }

    @Override
    public void close() {
        downstream.close();
    }
}
