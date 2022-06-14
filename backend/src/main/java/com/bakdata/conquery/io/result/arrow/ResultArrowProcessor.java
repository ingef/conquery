package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.io.result.arrow.ArrowRenderer.renderToStream;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.http.HttpStatus;

@UtilityClass
@Slf4j
public class ResultArrowProcessor {


	public static <E extends ManagedExecution<?> & SingleTableResult> Response getArrowResult(
			Function<OutputStream, Function<VectorSchemaRoot, ArrowWriter>> writerProducer,
			Subject subject,
			E exec,
			Dataset dataset,
			DatasetRegistry datasetRegistry,
			boolean pretty,
			String fileExtension,
			MediaType mediaType,
			ConqueryConfig config) {

		final Namespace namespace = datasetRegistry.get(dataset.getId());

		ConqueryMDC.setLocation(subject.getName());
		log.info("Downloading results for {} on dataset {}", exec, dataset);
		subject.authorize(dataset, Ability.READ);
		subject.authorize(dataset, Ability.DOWNLOAD);


		subject.authorize(exec, Ability.READ);

		// Check if subject is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(subject, exec);

		if (!(exec instanceof ManagedQuery || (exec instanceof ManagedForm && ((ManagedForm) exec).getSubQueries().size() == 1))) {
			return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Execution result is not a single Table").build();
		}

		// Get the locale extracted by the LocaleFilter


		IdPrinter idPrinter = config.getFrontend().getQueryUpload().getIdPrinter(subject, exec, namespace);
		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				datasetRegistry,
				config,
				idPrinter::createId
		);


		// Collect ResultInfos for id columns and result columns
		final List<ResultInfo> resultInfosId = config.getFrontend().getQueryUpload().getIdResultInfos();
		final List<ResultInfo> resultInfosExec = exec.getResultInfos();

		StreamingOutput out = output -> {
			try {
				renderToStream(
						writerProducer.apply(output),
						settings,
						config.getArrow().getBatchSize(),
						resultInfosId,
						resultInfosExec,
						exec.streamResults()
				);
			}
			finally {
				log.trace("DONE downloading data for `{}`", exec.getId());
			}
		};

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), fileExtension, mediaType, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}

}
