package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonView;

public interface View {

	interface Api extends View {}

	interface Persistence extends View {
		interface Manager extends Persistence {}

		interface Shard extends Persistence {}
	}

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
