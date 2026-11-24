package serap.annotations;


import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Documented
public @interface Asset {
    String identifier();
    boolean alwaysLoaded() default true;
    Dependency[] dependencies() default {};
}
