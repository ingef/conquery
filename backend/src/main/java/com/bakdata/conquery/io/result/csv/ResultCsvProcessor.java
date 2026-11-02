package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.OptionalLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultCsvProcessor {

	private final ConqueryConfig config;

	public Response createResult(ManagedExecutionId execId, Subject subject, boolean pretty, Charset charset, OptionalLong limit) {
		log.info("Downloading results for {}", execId);

		final ResultExportContext context = ResultExportPreparation.prepare(subject, execId, pretty, limit, config);

		final StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				final CsvRenderer renderer = new CsvRenderer(config.getCsv().createWriter(writer), context.printSettings());
				renderer.toCSV(context.idResultInfos(), context.resultInfos(), context.streamResults(), charset);
			}
			catch (EofException e) {
				log.trace("User canceled download");
			}
			catch (Exception e) {
				throw new WebApplicationException("Failed to load result", e);
			}
			finally {
				log.trace("FINISHED downloading {}", context.execution().getId());
			}
		};

		return makeResponseWithFileName(Response.ok(out),
										String.join(".", context.execution().getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_CSV),
										new MediaType("text", "csv", charset.toString()),
										ResultUtil.ContentDispositionOption.ATTACHMENT
		);

	}
}
