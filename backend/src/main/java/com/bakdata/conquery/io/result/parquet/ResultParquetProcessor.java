package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_PARQUET;

import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetProcessor {
	public static final MediaType PARQUET_MEDIA_TYPE = MediaType.valueOf(ResultParquetResource.PARQUET_MEDIA_TYPE_STRING);

	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig config;

	public Response createResultFile(Subject subject, ManagedExecution exec, boolean pretty) {

		ConqueryMDC.setLocation(subject.getName());

		final Dataset dataset = exec.getDataset();

		log.info("Downloading results for {} on dataset {}", exec, dataset);

		ResultUtil.authorizeExecutable(subject, exec);

		ResultUtil.checkSingleTableResult(exec);

		final Namespace namespace = datasetRegistry.get(dataset.getId());

		IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, exec, namespace, config.getIdColumns().getIds());

		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				namespace,
				config,
				idPrinter::createId
		);

		StreamingOutput out = output -> {

			final SingleTableResult singleTableResult = (SingleTableResult) exec;
			ParquetRenderer.writeToStream(
					output,
					config.getIdColumns().getIdResultInfos(),
					singleTableResult.getResultInfos(),
					settings,
					singleTableResult.streamResults()
			);

		};


		return makeResponseWithFileName(Response.ok(out), exec.getLabelWithoutAutoLabelSuffix(), FILE_EXTENTION_PARQUET, PARQUET_MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}
}
