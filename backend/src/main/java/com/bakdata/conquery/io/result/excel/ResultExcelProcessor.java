package com.bakdata.conquery.io.result.excel;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ExcelConfig;
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

@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class ResultExcelProcessor {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig conqueryConfig;

	private final ExcelConfig excelConfig;


	public <E extends ManagedExecution<?> & SingleTableResult> Response createResult(Subject subject, E exec, boolean pretty) {
		ConqueryMDC.setLocation(subject.getName());

		final Dataset dataset = exec.getDataset();

		log.info("Downloading results for {} on dataset {}", exec, dataset);

		ResultUtil.authorizeExecutable(subject, exec, dataset);
		ResultUtil.checkSingleTableResult(exec);
		subject.authorize(dataset, Ability.DOWNLOAD);

		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final IdPrinter idPrinter = conqueryConfig.getFrontend().getQueryUpload().getIdPrinter(subject, exec, namespace);

		final Locale locale = I18n.LOCALE.get();
		final PrintSettings settings = new PrintSettings(pretty, locale, datasetRegistry, conqueryConfig, idPrinter::createId);

		final ExcelRenderer excelRenderer = new ExcelRenderer(excelConfig, settings);

		final StreamingOutput out = output -> {
			excelRenderer.renderToStream(conqueryConfig.getFrontend().getQueryUpload().getIdResultInfos(), exec, output);
			log.trace("FINISHED downloading {}", exec.getId());
		};

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "xlsx", MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}


}
