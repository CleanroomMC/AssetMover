package io.github.cleanroommc.assetmover;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface InternalModAnnotation {

    String modid();

    String name();

    String version();

}
