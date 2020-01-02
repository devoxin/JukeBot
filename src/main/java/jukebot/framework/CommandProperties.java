package jukebot.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProperties {
    String[] aliases() default {};

    String description() default "No description available";

    CommandCategory category() default CommandCategory.MISC;

    boolean developerOnly() default false;

    boolean nsfw() default false;

    boolean enabled() default true;
}
