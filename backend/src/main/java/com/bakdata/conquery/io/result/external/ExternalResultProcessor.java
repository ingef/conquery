package com.bakdata.conquery.io.result.external;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExternalResultProcessor {

	private final DatasetRegistry<?> datasetRegistry;

	public <T extends ManagedExecution & ExternalResult> Response getResult(Subject subject, ExternalExecution execution, String fileName) {

		ResultUtil.authorizeExecutable(subject, execution);

		ExecutionManager<?> executionManager = datasetRegistry.get(execution.getDataset()).getExecutionManager();
		ExternalResult externalResult = executionManager.getExternalResult(execution.getId());

		return externalResult.fetchExternalResult(fileName);
	}
}
