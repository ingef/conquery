package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Json views that allow fine grain control over serialization of fields.
 *
 * Basically the differentiations are:
 * - is a field internal or external (REST api) visible
 * - should a field be persistent or transient
 * - should a field be sent to the shards/manager
 *
 * By default, a field has no view and written and expected/read by all mappers, as {@link com.fasterxml.jackson.databind.MapperFeature#DEFAULT_VIEW_INCLUSION} is enabled (the default).
 */
public interface View {

	/**
	 * View used by the mapper in the REST api.
	 *
	 * @see View.ApiManagerPersistence for classes that is propagated form the api unto the shards,
	 * but have fields that should not be sent to the shards.
	 */
	interface Api extends View {}

	/**
	 * View class for fields that should be written to a storage.
	 *
	 * @see View.Internal for fields that are shared between manager and shards and should be persisted.
	 */
	interface Persistence extends View {
		interface Manager extends Persistence {}

		interface Shard extends Persistence {}
	}

	/**
	 * View class for fields that are transmitted between manager und shards.
	 *
	 * @see View.Internal for fields that are shared between manager and shards and should be persisted.
	 */
	interface InternalCommunication extends View {}

	/**
	 * Meta annotation for fields that are internally created, used, persisted and shared between manager node and shard nodes,
	 * but which never leave through the REST api.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonView({View.InternalCommunication.class, View.Persistence.class})
	@interface Internal {}

	/**
	 * Meta annotation for fields that are written or read through the REST api and stored on the manager, but never touch a shard node.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonView({View.Api.class, View.Persistence.Manager.class})
	@interface ApiManagerPersistence {}
}
