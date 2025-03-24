package com.bakdata.conquery.models.messages.network;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.messages.Message;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public abstract class NetworkMessage<CTX extends NetworkMessageContext<?>> implements Message {

	@ToString.Include
	@NotNull
	private UUID messageId = UUID.randomUUID();

	public abstract void react(CTX context) throws Exception;

}
