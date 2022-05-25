package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An annotation that guides Jackson to serialize/deserialize the field as a {@link com.bakdata.conquery.models.identifiable.ids.AId} instead of the object content itself.
 *
 * @implNote You cannot expect MetaIdRefs to work beyond the ManagerNode! So resolve the content you need on the Manager (Or implement the necessary logic).
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty
@JsonSerialize(using = IdReferenceSerializer.class)
@JsonDeserialize(using = MetaIdReferenceDeserializer.class)
@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface MetaIdRef {
}