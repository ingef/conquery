package com.bakdata.conquery.quarkus.models;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolymorphicModelBase {

	String discriminator() default "type";

	String schemaName();

	String description() default "";
}
