/*
   Copyright 2023 devoxin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package me.devoxin.jukebot.audio.filters;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

public class ReverbFilter implements FloatPcmAudioFilter {
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final float[][] delayBuffer;
    private final int[] delayBufferIndex;
    private final float decay;
    private final FloatPcmAudioFilter downstream;

    public ReverbFilter(FloatPcmAudioFilter downstream, float decay, int channelCount) {
        this.decay = decay;
        this.delayBuffer = new float[channelCount][DEFAULT_BUFFER_SIZE];
        this.delayBufferIndex = new int[channelCount];
        this.downstream = downstream;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        for (int i = 0; i < input.length; i++) {
            processChannel(i, input[i], offset, length, input[i]);
        }

        downstream.process(input, offset, length);
    }

    private void processChannel(int channel, float[] input, int offset, int length, float[] output) {
        for (int i = offset; i < offset + length; i++) {
            int index = delayBufferIndex[channel];
            float inputSample = input[i];
            float outputSample = inputSample + delayBuffer[channel][index];
            delayBuffer[channel][index] = inputSample * decay + delayBuffer[channel][index] * (1.0f - decay);
            delayBufferIndex[channel] = (index + 1) % DEFAULT_BUFFER_SIZE;
            output[i] = outputSample;
        }
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {

    }

    @Override
    public void flush() {
        for (int channel = 0; channel < delayBuffer.length; channel++) {
            for (int i = 0; i < DEFAULT_BUFFER_SIZE; i++) {
                delayBuffer[channel][i] = 0.0f;
            }

            delayBufferIndex[channel] = 0;
        }
    }

    @Override
    public void close() {

    }
}
