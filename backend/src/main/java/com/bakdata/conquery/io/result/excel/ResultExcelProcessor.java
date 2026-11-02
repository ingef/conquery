package com.bakdata.conquery.io.result.excel;

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
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class ResultExcelProcessor {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ConqueryConfig conqueryConfig;

	private final ExcelConfig excelConfig;

	public Response createResult(Subject subject, ManagedExecutionId execId, boolean pretty, OptionalLong limit) {
		log.info("Downloading results for {}", execId);

		final ResultExportContext context = ResultExportPreparation.prepare(subject, execId, pretty, limit, conqueryConfig);

		final ExcelRenderer excelRenderer = new ExcelRenderer(excelConfig, context.printSettings());

		final StreamingOutput out = output -> {
			excelRenderer.renderToStream(
					context.idResultInfos(),
					(ManagedExecution & SingleTableResult) context.execution(),
					output,
					context.limit()
			);
			log.trace("FINISHED downloading {}", context.execution().getId());
		};

		return makeResponseWithFileName(Response.ok(out), String.join(".", context.execution().getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_XLSX), MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}


}
