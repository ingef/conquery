package com.bakdata.conquery.io.result.external;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.io.result.ExternalExecutionInfo;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExternalResultProcessor {

	private final DatasetRegistry<?> datasetRegistry;

	public Response getResult(Subject subject, ManagedExecutionId execId, String fileName) {

		ManagedExecution execution = execId.resolve();
		ResultUtil.authorizeExecutable(subject, execution);

		ExecutionManager executionManager = datasetRegistry.get(execId.getDataset()).getExecutionManager();
		ExternalExecutionInfo externalResult = executionManager.getExecutionInfo(execution.getId());

		return externalResult.fetchExternalResult(fileName);
	}
}
