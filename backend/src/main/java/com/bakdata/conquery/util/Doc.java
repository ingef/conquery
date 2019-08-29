package com.bakdata.conquery.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Doc {

	String description() default "";
	String example() default "";
}
