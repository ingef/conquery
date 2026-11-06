package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultExportContext;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.resources.ResourceConstants;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
public class ResultCsvProcessor {

	public Response createResult(ResultExportContext context, Charset charset, CSVConfig config) {
		log.info("Downloading results for {}", context.execution().getId());

		final StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				final CsvRenderer renderer = new CsvRenderer(config.createWriter(writer), context.printSettings());
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
