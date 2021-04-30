package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.io.result.excel.ExcelRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.ResourceUtil;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

@RequiredArgsConstructor
public class ResultExcelProcessor {

	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig config;


	public Response getExcelResult(User user, ManagedExecutionId execId, DatasetId datasetId, boolean pretty) {
		ConqueryMDC.setLocation(user.getName());
		Dataset dataset = datasetRegistry.get(datasetId).getDataset();
		user.authorize(dataset, Ability.READ);
		user.authorize(dataset, Ability.DOWNLOAD);


		ManagedExecution<?> exec = datasetRegistry.getMetaStorage().getExecution(execId);

		ResourceUtil.throwNotFoundIfNull(execId, exec);


		user.authorize(exec, Ability.READ);

		IdMappingConfig idMapping = config.getIdMapping();
		IdMappingState mappingState = idMapping.initToExternal(user, exec);
		PrintSettings settings = new PrintSettings(
				pretty,
				I18n.LOCALE.get(),
				datasetRegistry,
				config,
				(EntityResult cer) -> ResultUtil.createId(datasetRegistry.get(dataset.getId()), cer, idMapping, mappingState));

		StreamingOutput out = output -> ExcelRenderer.renderToStream(
				settings,
				idMapping.getPrintIdFields(),
				exec,
				output

		);

		return makeResponseWithFileName("xlsx", exec, out).build();
	}
}
