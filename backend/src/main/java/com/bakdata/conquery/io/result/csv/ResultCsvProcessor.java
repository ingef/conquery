package com.bakdata.conquery.io.result.csv;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.ResultProcessor;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.ResourceUtil;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

@Slf4j
@RequiredArgsConstructor
public class ResultCsvProcessor {

	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig config;


	public Response.ResponseBuilder getResult(User user, DatasetId datasetId, ManagedExecutionId queryId, String userAgent, String queryCharset, boolean pretty, String fileExtension) {
		final Namespace namespace = datasetRegistry.get(datasetId);
		ConqueryMDC.setLocation(user.getName());
		log.info("Downloading results for {} on dataset {}", queryId, datasetId);
		user.authorize(namespace.getDataset(), Ability.READ);
		user.authorize(namespace.getDataset(), Ability.DOWNLOAD);

		ManagedExecution<?> exec = datasetRegistry.getMetaStorage().getExecution(queryId);

		ResourceUtil.throwNotFoundIfNull(queryId, exec);

		user.authorize(exec, Ability.READ);

		// Check if user is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(user, exec);

		IdMappingState mappingState = config.getIdMapping().initToExternal(user, exec);

		// Get the locale extracted by the LocaleFilter
		PrintSettings settings = new PrintSettings(
				pretty,
				I18n.LOCALE.get(),
				datasetRegistry,
				config,
				cer -> ResultUtil.createId(namespace, cer, config.getIdMapping(), mappingState)
		);
		Charset charset = ResultProcessor.determineCharset(userAgent, queryCharset);


		StreamingOutput out =  os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				CsvRenderer renderer = new CsvRenderer(config.getCsv().createWriter(writer), settings);
				renderer.toCSV(config.getIdMapping().getPrintIdFields(), exec.getResultInfo(), exec.streamResults());
			}
			catch (EofException e) {
				log.info("User canceled download");
			}
			catch (Exception e) {
				throw new WebApplicationException("Failed to load result", e);
			}finally {
				ConqueryMDC.clearLocation();
			}
		};
		return ResultProcessor.makeResponseWithFileName(fileExtension, exec, out);
	}

}
