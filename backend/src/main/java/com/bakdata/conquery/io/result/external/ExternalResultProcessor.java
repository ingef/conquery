package com.bakdata.conquery.io.result.external;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.io.result.ExternalState;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExternalResultProcessor {

	private final DatasetRegistry<?> datasetRegistry;

	public Response getResult(Subject subject, ManagedExecutionId execId, String fileName) {

		ResultUtil.authorizeExecutable(subject, execId.resolve());

		ExecutionManager executionManager = datasetRegistry.get(execId.getDataset()).getExecutionManager();
		ExternalState externalResult = executionManager.getResult(execId);

		return externalResult.fetchExternalResult(fileName);
	}
}
