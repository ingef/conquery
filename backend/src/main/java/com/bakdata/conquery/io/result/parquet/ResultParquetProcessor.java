package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.util.OptionalLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultExportContext;
import com.bakdata.conquery.io.result.ResultExportPreparation;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultParquetProcessor {
	public static final MediaType PARQUET_MEDIA_TYPE = MediaType.valueOf(ResultParquetResource.PARQUET_MEDIA_TYPE_STRING);

	private final ConqueryConfig config;

	public Response createResultFile(Subject subject, ManagedExecutionId execId, boolean pretty, OptionalLong limit) {

		log.info("Downloading results for {}", execId);

		final ResultExportContext context = ResultExportPreparation.prepare(subject, execId, pretty, limit, config);

		final StreamingOutput out = output -> {

			ParquetRenderer.writeToStream(
					output,
					context.idResultInfos(),
					context.resultInfos(),
					context.printSettings(),
					context.streamResults()
			);

		};


		return makeResponseWithFileName(Response.ok(out),
										String.join(".", context.execution().getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_PARQUET),
										PARQUET_MEDIA_TYPE,
										ResultUtil.ContentDispositionOption.ATTACHMENT
		);
	}
}
