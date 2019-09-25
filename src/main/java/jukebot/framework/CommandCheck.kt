package jukebot.framework

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.CLASS)
annotation class CommandCheck(
    val dj: DjCheck = DjCheck.NONE,
    val donor: Int = 0,
    val isPlaying: Boolean = false
)
