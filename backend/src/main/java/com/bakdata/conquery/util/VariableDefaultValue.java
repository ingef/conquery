package com.bakdata.conquery.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Marker annotation for the AutoDoc project to signal that this fields default value is not stable because it is e.g. randomly generated or the current time.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface VariableDefaultValue {

}