package jukebot.framework

import java.lang.reflect.Method

class MethodWrapper(val description: String, private val method: Method, private val cls: Command) {

    fun invoke(ctx: Context) {
        method.invoke(cls, ctx)
    }

}
