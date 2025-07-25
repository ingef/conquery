package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.OptionalLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
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
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultCsvProcessor {

	private final ConqueryConfig config;

	public <E extends ManagedExecution & SingleTableResult> Response createResult(E exec, Subject subject, boolean pretty, Charset charset, OptionalLong limit) {

		ManagedExecutionId execId = exec.getId();
		final Namespace namespace = exec.getNamespace();


		ConqueryMDC.setLocation(subject.getName());
		log.info("Downloading results for {}", execId);

		ResultUtil.authorizeExecutable(subject, exec);

		// Check if subject is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(subject, exec);

		// Initialize execution so columns can be correctly accounted
		exec.initExecutable();

		final IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, exec, namespace, config.getIdColumns().getIds());

		// Get the locale extracted by the LocaleFilter
		final Locale locale = I18n.LOCALE.get();
		final PrintSettings settings = new PrintSettings(pretty, locale, namespace, config, idPrinter::createId, null);

		final StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				final CsvRenderer renderer = new CsvRenderer(config.getCsv().createWriter(writer), settings);
				renderer.toCSV(config.getIdColumns().getIdResultInfos(), exec.getResultInfos(), exec.streamResults(limit), settings, charset);
			}
			catch (EofException e) {
				log.trace("User canceled download");
			}
			catch (Exception e) {
				throw new WebApplicationException("Failed to load result", e);
			}
			finally {
				log.trace("FINISHED downloading {}", exec.getId());
			}
		};

		return makeResponseWithFileName(Response.ok(out),
										String.join(".", exec.getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_CSV),
										new MediaType("text", "csv", charset.toString()),
										ResultUtil.ContentDispositionOption.ATTACHMENT
		);

	}
}
