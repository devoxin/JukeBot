package jukebot.framework

import java.lang.reflect.Method

class MethodWrapper(val description: String, private val method: Method, private val cls: Command) {
    fun invoke(ctx: Context, withArgs: Boolean = false) {
        if (withArgs) {
            method.invoke(cls, ctx, ctx.args.drop(1))
        } else {
            method.invoke(cls, ctx)
        }
    }
}
