package me.devoxin.jukebot.audio.filters.unusable;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

public class FlangerFilter implements FloatPcmAudioFilter {
    private float[] flangerBuffer;
    private int writePosition;

    private final FloatPcmAudioFilter downstream;
    private final double maxFlangerLength;
    private final double wet;
    private final double dry;
    private final double sampleRate;
    private final double lfoFrequency;

    private double time = 0.0;

    public FlangerFilter(FloatPcmAudioFilter downstream, double maxFlangerLength, double wet, int sampleRate, double lfoFrequency) {
        this.flangerBuffer = new float[(int) (sampleRate * maxFlangerLength)];
        this.downstream = downstream;
        this.maxFlangerLength = maxFlangerLength;
        this.wet = wet;
        this.dry = (float) (1 - wet);
        this.sampleRate = sampleRate;
        this.lfoFrequency = lfoFrequency;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        // Divide f by two, to counter rectifier below, which effectively
        // doubles the frequency
        double twoPIf = 2 * Math.PI * lfoFrequency / 2.0;
        double timeStep = 1.0 / sampleRate; // also in seconds

        for (int i = offset; i < offset + length; i++) {
            // Calculate the LFO delay value with a sine wave:
            //fix by hans bickel
            double lfoValue = (flangerBuffer.length - 1) * Math.sin(twoPIf * time);
            // add a time step, each iteration
            time += timeStep;

            // Make the delay a positive integer, sine rectifier
            int delay = (int) (Math.round(Math.abs(lfoValue)));

            // store the current sample in the delay buffer;
            if (writePosition >= flangerBuffer.length) {
                writePosition = 0;
            }
            flangerBuffer[writePosition] = input[0][i];

            // find out the position to read the delayed sample:
            int readPosition = writePosition - delay;

            if (readPosition < 0) {
                readPosition += flangerBuffer.length;
            }

            //increment the write-position
            writePosition++;

            for (int channel = 0; channel < input.length; channel++) {
                input[channel][i] = (float) (dry * input[channel][i] + wet * flangerBuffer[readPosition]);
            }

            downstream.process(input, offset, length);
        }
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }
}
