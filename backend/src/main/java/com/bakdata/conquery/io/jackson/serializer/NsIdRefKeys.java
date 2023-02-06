package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Annotation for Maps where the keys are supposed to be NsIdRefs
 */
@JacksonAnnotationsInside
@JsonDeserialize(keyUsing = NsIdReferenceKeyDeserializer.class)
@JsonSerialize(keyUsing = IdReferenceKeySerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NsIdRefKeys {
}
