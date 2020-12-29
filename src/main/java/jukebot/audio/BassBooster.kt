/*
   Copyright 2019 Devoxin

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

// ==========
// Basically, don't steal this and we won't have a problem.
// ==========

package jukebot.audio

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import java.text.DecimalFormat

class BassBooster(private val player: AudioPlayer) : EqualizerFactory() {
    var percentage: Float = 0.0f
        private set

    val pcString: String
        get() = dpFormatter.format(percentage)

    val isEnabled: Boolean
        get() = percentage != 0.0f

    fun boost(pc: Float) {
        val lastPc = percentage
        this.percentage = pc

        if (pc == 0.0f) {
            return player.setFilterFactory(null)
        }

        if (lastPc == 0.0f) {
            player.setFilterFactory(this)
        }

        val multiplier = pc / 100

        for ((band, gain) in freqGains) {
            this.setGain(band, gain * multiplier)
        }
    }

    companion object {
        private val dpFormatter = DecimalFormat("0.00")

        private val freqGains = mapOf(
            0 to -0.05f,  // 25 Hz
            1 to 0.07f,   // 40 Hz
            2 to 0.16f,   // 63 Hz
            3 to 0.03f,   // 100 Hz
            4 to -0.05f,  // 160 Hz
            5 to -0.11f   // 250 Hz
        )

//        OFF(0F, 0F, 0F),
//        WEAK(0.03F, 0.01F, 0.0F),
//        MEDIUM(0.1F, 0.08F, 0.04F),
//        STRONG(0.2F, 0.15F, 0.11F),
//        INSANE(0.4F, 0.26F, 0.18F),
//        WTF(1F, 0.8F, 0.6F);
    }
}
