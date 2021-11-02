package com.bakdata.conquery.io.result.excel;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.util.ArrayList;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultExcelProcessor {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig config;


	public <E extends ManagedExecution<?> & SingleTableResult> Response getExcelResult(User user, E exec, DatasetId datasetId, boolean pretty) {
		ConqueryMDC.setLocation(user.getName());
		final Namespace namespace = datasetRegistry.get(datasetId);
		Dataset dataset = namespace.getDataset();

		user.authorize(dataset, Ability.READ);
		user.authorize(dataset, Ability.DOWNLOAD);
		user.authorize(exec, Ability.READ);

		IdPrinter idPrinter = config.getFrontend().getQueryUpload().getIdPrinter(user,exec,namespace);

		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				datasetRegistry,
				config,
				idPrinter::createId
		);

		ExcelRenderer excelRenderer = new ExcelRenderer(config.getExcel(), settings);

		final ArrayList<ResultInfo> resultInfosId = new ArrayList<>();

		StreamingOutput out = output -> excelRenderer.renderToStream(resultInfosId,
				(ManagedExecution<?> & SingleTableResult)exec,
				output
		);

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "xlsx", MEDIA_TYPE, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}

}
