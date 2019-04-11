package com.bakdata.conquery.models.messages.namespaces;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.messages.Message;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase @Getter @Setter
public abstract class NamespacedMessage<CTX> implements Message {

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	public abstract void react(CTX context) throws Exception;
	
}
