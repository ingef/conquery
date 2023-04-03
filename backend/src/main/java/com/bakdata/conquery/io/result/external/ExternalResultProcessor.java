package com.bakdata.conquery.io.result.external;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExternalResultProcessor {

	public <T extends ManagedExecution & ExternalResult> Response getResult(Subject subject, ManagedExecution execution, String fileName) {

		ResultUtil.authorizeExecutable(subject, execution);

		if (!(execution instanceof ExternalResult)) {
			throw new WebApplicationException("The execution exists, but produces not a zipped result", Response.Status.CONFLICT);

		}

		T externalExecution = (T) execution;

		return externalExecution.fetchExternalResult(fileName);
	}
}
