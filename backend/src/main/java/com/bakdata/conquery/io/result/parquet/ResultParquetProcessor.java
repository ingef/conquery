package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_PARQUET;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetProcessor {
	public static final MediaType PARQUET_MEDIA_TYPE = MediaType.valueOf(ResultParquetResource.PARQUET_MEDIA_TYPE_STRING);

	private final DatasetRegistry datasetRegistry;

	public Response createResultFile(Subject subject, ManagedExecution<?> exec, boolean pretty) {

		ConqueryMDC.setLocation(subject.getName());

		final Dataset dataset = exec.getDataset();

		log.info("Downloading results for {} on dataset {}", exec, dataset);
		ResultUtil.authorizeExecutable(subject, exec, dataset);

		ResultUtil.checkSingleTableResult(exec);

		return ResultUtil.makeResponseWithFileName(null, exec.getLabelWithoutAutoLabelSuffix(), FILE_EXTENTION_PARQUET, PARQUET_MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}
}
