package me.devoxin.jukebot.annotations

object Prerequisites {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TriggerConnect

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RequireMutualVoiceChannel
}
