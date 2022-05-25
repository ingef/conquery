package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An annotation that guides Jackson to serialize/deserialize the field as a {@link Id} instead of the object content itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty
@JsonSerialize(contentUsing=IdReferenceSerializer.class)
@JsonDeserialize(contentUsing=MetaIdReferenceDeserializer.class)
@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface MetaIdRefCollection {}