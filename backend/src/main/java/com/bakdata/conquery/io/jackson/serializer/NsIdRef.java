package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An annotation that guides Jackson to serialize/deserialize the field as a {@link NamespacedId} instead of the object content itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty
@JsonSerialize(using = IdReferenceSerializer.class)
@JsonDeserialize(using = NsIdReferenceDeserializer.class)
@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface NsIdRef {
	/**
	 * Sets the scope on which nodes a {@link NamespacedId} should be resolved.
	 * If a reference is encountered on a node outside the scope (e.g. {@code @NsIdRef(serdesTarget=MANAGER)} on a {@link com.bakdata.conquery.commands.ShardNode}) the field will resolve to {@code null}.
	 */
	SerdesTarget serdesTarget () default SerdesTarget.MANAGER_AND_SHARD;
}