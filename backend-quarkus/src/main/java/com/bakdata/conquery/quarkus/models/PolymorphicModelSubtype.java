package com.bakdata.conquery.quarkus.models;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the serialized identity of a concrete polymorphic model.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolymorphicModelSubtype {

	Class<?> base();

	String id();
}
