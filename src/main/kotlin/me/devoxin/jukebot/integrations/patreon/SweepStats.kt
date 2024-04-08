package me.devoxin.jukebot.integrations.patreon

data class SweepStats(
    val total: Int,
    val changed: Int,
    val removed: Int,
    val fatal: Int
) {

    override fun toString(): String {
        return "SweepStats[total=$total, changed=$changed, removed=$removed, fatal=$fatal]"
    }

}
