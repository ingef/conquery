package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Slf4j
@Data
@CPSType(base = ResultRendererProvider.class, id = "CSV")
public class CsvResultRenderer implements ResultRendererProvider {

	@JsonIgnore
	private DatasetRegistry datasetRegistry;

	@JsonIgnore
	private ConqueryConfig config;


	private boolean hidden = false;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(ResultCsvResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public Response createResult(Subject subject, ManagedExecution<?> execRaw, Dataset dataset, boolean pretty, Charset charset, Runnable onClose) {

		final ManagedQuery exec = (ManagedQuery) execRaw;

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
				onClose.run();
			}
		};

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "csv", new MediaType("text", "csv", charset.toString()), ResultUtil.ContentDispositionOption.ATTACHMENT);

	}

	@Override
	public void registerResultResource(JerseyEnvironment environment, ManagerNode manager) {
		setDatasetRegistry(manager.getDatasetRegistry());
		setConfig(manager.getConfig());

		CsvResultRenderer me = this;

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(me).to(CsvResultRenderer.class);
			}
		});

		environment.register(ResultCsvResource.class);
	}
}
