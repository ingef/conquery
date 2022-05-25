package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.io.result.excel.ExcelRenderer;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.ResultExcelResource;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Data
@CPSType(base = ResultRendererProvider.class, id = "XLSX")
@Slf4j
public class ExcelResultProvider implements ResultRendererProvider {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

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

		return List.of(ResultExcelResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public void registerResultResource(JerseyEnvironment environment, ManagerNode manager) {
		setConfig(manager.getConfig());
		setDatasetRegistry(manager.getDatasetRegistry());

		//inject required services
		environment.register(this);

		environment.register(ResultExcelResource.class);
	}

	public Response createResult(Subject subject, ManagedExecution<?> exec, Dataset dataset, boolean pretty) {
		ConqueryMDC.setLocation(subject.getName());
		final Namespace namespace = datasetRegistry.get(dataset.getId());

		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.DOWNLOAD);
		subject.authorize(exec, Ability.READ);

		IdPrinter idPrinter = config.getFrontend().getQueryUpload().getIdPrinter(subject, exec, namespace);

		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				datasetRegistry,
				config,
				idPrinter::createId
		);

		ExcelRenderer excelRenderer = new ExcelRenderer(config.getExcel(), settings);

		StreamingOutput out = output -> {
			excelRenderer.renderToStream(
					config.getFrontend().getQueryUpload().getIdResultInfos(),
					(ManagedExecution<?> & SingleTableResult) exec,
					output
			);
			log.trace("FINISHED downloading {}", exec.getId());
		};

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "xlsx", MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}
}
