package com.bakdata.conquery.models.messages.network;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.messages.Message;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class NetworkMessage<CTX extends NetworkMessageContext<?>> implements Message {

	@Getter @Setter @NotNull
	private UUID messageId = UUID.randomUUID();

	public abstract void react(CTX context) throws Exception;
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" [id=" + messageId + "]";
	}
}
