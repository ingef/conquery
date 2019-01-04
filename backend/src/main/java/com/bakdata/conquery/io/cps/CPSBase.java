package com.bakdata.conquery.io.cps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@JacksonAnnotationsInside @JsonTypeIdResolver(CPSTypeIdResolver.class)
public @interface CPSBase {
}