package me.devoxin.jukebot.integrations.patreon.entities

enum class PatronStatus {
    DECLINED_PATRON,
    ACTIVE_PATRON,
    FORMER_PATRON,
    UNKNOWN;

    companion object {
        fun from(value: String): PatronStatus {
            return when (value) {
                "declined_patron" -> DECLINED_PATRON
                "active_patron" -> ACTIVE_PATRON
                "former_patron" -> FORMER_PATRON
                else -> UNKNOWN
            }
        }
    }
}
