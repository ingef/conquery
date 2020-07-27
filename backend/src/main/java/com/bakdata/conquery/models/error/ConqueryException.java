package com.bakdata.conquery.models.error;

import lombok.Getter;
import lombok.NonNull;

@SuppressWarnings("serial")
@Getter
public class ConqueryException extends IllegalStateException {
	@NonNull
	private final ConqueryError ctx;
	
	
	public ConqueryException(ConqueryError ctx) {
		super(ctx.toString());
		this.ctx = ctx;
	}
}
