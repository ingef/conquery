package com.bakdata.conquery.quarkus.plugin.api.models;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolymorphicModelBase {
	String discriminator() default "type";
	String schemaName();
	String description() default "";
}
