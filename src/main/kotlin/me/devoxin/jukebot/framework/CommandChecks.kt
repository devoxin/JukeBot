package me.devoxin.jukebot.framework

object CommandChecks {
    annotation class Dj(val alone: Boolean)
    annotation class Playing
    annotation class Donor(val tier: Int)
}
