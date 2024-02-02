package me.devoxin.jukebot.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ComponentId(
    val value: String
)
