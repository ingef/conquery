package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonView;

/** Jackson view for fields only used in the manager slave connection **/
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonView(InternalOnly.class)
public @interface InternalOnly {}