package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.execution.ExecutionError.ConqueryExecutionError;
import lombok.Getter;
import lombok.NonNull;

@SuppressWarnings("serial")
@Getter
public class ExecutionException extends IllegalStateException {
	@NonNull
	private final ConqueryExecutionError ctx;
	
	
	public ExecutionException(ConqueryExecutionError ctx) {
		super(ctx.toString());
		this.ctx = ctx;
	}
}
