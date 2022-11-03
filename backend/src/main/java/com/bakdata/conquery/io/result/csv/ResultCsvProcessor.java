package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultCsvProcessor {

	private final ConqueryConfig config;
	private final DatasetRegistry datasetRegistry;

	public <E extends ManagedExecution<?> & SingleTableResult> Response createResult(Subject subject, E exec, Dataset dataset, boolean pretty, Charset charset) {

		final Namespace namespace = datasetRegistry.get(dataset.getId());

		ConqueryMDC.setLocation(subject.getName());
		log.info("Downloading results for {} on dataset {}", exec, dataset);
		subject.authorize(namespace.getDataset(), Ability.READ);
		subject.authorize(namespace.getDataset(), Ability.DOWNLOAD);

		subject.authorize(exec, Ability.READ);

		// Check if subject is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(subject, exec);

		IdPrinter idPrinter = config.getFrontend().getQueryUpload().getIdPrinter(subject, exec, namespace);


		// Get the locale extracted by the LocaleFilter
		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				datasetRegistry,
				config,
				idPrinter::createId
		);

		StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				CsvRenderer renderer = new CsvRenderer(config.getCsv().createWriter(writer), settings);
				renderer.toCSV(config.getFrontend().getQueryUpload().getIdResultInfos(), exec.getResultInfos(), exec.streamResults());
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

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "csv", new MediaType("text", "csv", charset.toString()), ResultUtil.ContentDispositionOption.ATTACHMENT);

	}
}
