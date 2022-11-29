package com.bakdata.conquery.io.result.external;


import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.ResourceUtil;
import it.unimi.dsi.fastutil.Pair;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExternalResultProcessor {


	private final DatasetRegistry datasetRegistry;
	private final MetaStorage storage;

	public <T extends ManagedExecution<?> & ExternalResult> Response getResult(Subject subject, DatasetId datasetId, ManagedExecutionId executionId, String fileName, String fileExtension) {
		Dataset dataset = datasetRegistry.get(datasetId).getDataset();
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.DOWNLOAD);

		ManagedExecution<?> execution = storage.getExecution(executionId);

		ResourceUtil.throwNotFoundIfNull(executionId, execution);

		subject.authorize(execution, Ability.READ);

		if (!(execution instanceof ExternalResult)) {
			throw new WebApplicationException("The execution exists, but produces not a zipped result", Response.Status.CONFLICT);

		}

		T externalExecution = (T) execution;

		Pair<StreamingOutput, MediaType> out = externalExecution.getExternalResult(new ResultFileReference(fileName, fileExtension));

		return makeResponseWithFileName(out.key(), fileName, fileExtension, out.value(), ResultUtil.ContentDispositionOption.INLINE);
	}

	public record ResultFileReference(String fileName, String fileExtension) {
	}
}
