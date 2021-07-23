package com.bakdata.conquery.io.result.excel;

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
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

@RequiredArgsConstructor
public class ResultExcelProcessor {

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

		PrintSettings settings = new PrintSettings(
				pretty,
				I18n.LOCALE.get(),
				datasetRegistry,
				config,
				idPrinter::createId
		);

		ExcelRenderer excelRenderer = new ExcelRenderer(config.getExcel());

		StreamingOutput out = output -> excelRenderer.renderToStream(
				settings,
				config.getFrontend().getQueryUpload().getPrintIdFields(),
				(ManagedExecution<?> & SingleTableResult)exec,
				output
		);

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), "xlsx");
	}

}
