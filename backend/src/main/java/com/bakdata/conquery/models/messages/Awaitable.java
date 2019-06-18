package com.bakdata.conquery.models.messages;


public interface Awaitable {

	void awaitSuccess();

	void awaitAnyResult();

}
