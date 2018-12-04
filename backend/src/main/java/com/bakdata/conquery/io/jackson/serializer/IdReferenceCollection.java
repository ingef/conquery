package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty
@JsonSerialize(contentUsing=IdReferenceSerializer.class)
@JsonDeserialize(contentUsing=IdReferenceDeserializer.class)
public @interface IdReferenceCollection {

}
