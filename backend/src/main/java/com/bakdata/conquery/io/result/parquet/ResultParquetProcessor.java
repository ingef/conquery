package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.util.Locale;
import java.util.OptionalLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetProcessor {
	public static final MediaType PARQUET_MEDIA_TYPE = MediaType.valueOf(ResultParquetResource.PARQUET_MEDIA_TYPE_STRING);

	private final DatasetRegistry<?> datasetRegistry;
	private final ConqueryConfig config;

	public Response createResultFile(Subject subject, ManagedExecutionId execId, boolean pretty, OptionalLong limit) {

		ConqueryMDC.setLocation(subject.getName());

		final ManagedExecution execution = execId.resolve();

		log.info("Downloading results for {}", execId);

		ResultUtil.authorizeExecutable(subject, execution);

		ResultUtil.checkSingleTableResult(execution);

		final Namespace namespace = datasetRegistry.get(execution.getDataset());

		final IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, execution, namespace, config.getIdColumns().getIds());

		final Locale locale = I18n.LOCALE.get();
		final PrintSettings settings = new PrintSettings(pretty, locale, namespace, config, idPrinter::createId, null);

		final StreamingOutput out = output -> {

			final SingleTableResult singleTableResult = (SingleTableResult) execution;
			ParquetRenderer.writeToStream(
					output,
					config.getIdColumns().getIdResultInfos(),
					singleTableResult.getResultInfos(),
					settings,
					singleTableResult.streamResults(limit)
			);

		};


		return makeResponseWithFileName(Response.ok(out), String.join(".", execution.getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_PARQUET), PARQUET_MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}
}
