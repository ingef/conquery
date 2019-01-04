package com.bakdata.conquery.models.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Message {

	@JsonIgnore
	default boolean isSlowMessage() {
		return false;
	}
}
