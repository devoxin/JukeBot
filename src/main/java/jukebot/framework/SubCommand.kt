package jukebot.framework


@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
        val trigger: String,
        val description: String
)
