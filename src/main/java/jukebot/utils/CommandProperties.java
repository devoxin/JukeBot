package jukebot.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProperties {
    String[] aliases() default {};
    boolean developerOnly() default false;
    String description() default "No description available";
    category category() default category.MISC;

    enum category {
        CONTROLS, MEDIA, MISC
    }

}