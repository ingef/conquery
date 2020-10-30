package com.bakdata.conquery.models.execution;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

/**
 * Holder for utility methods to obtain an result from an execution.
 * Acts as a bridge between HTTP-requests and {@link ManagedExecution}s.
 */
@Slf4j
public class ResultProcessor {
	
	public static ResponseBuilder getResult(User user, DatasetId datasetId, ManagedExecutionId queryId, String userAgent, String queryCharset, boolean pretty, DatasetRegistry datasetRegistry, ConqueryConfig config, String fileExtension) {
		ConqueryMDC.setLocation(user.getName());
		log.info("Downloading results for {} on dataset {}", queryId, datasetId);
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		ManagedExecution<?> exec = datasetRegistry.getMetaStorage().getExecution(queryId);
		
		// Check if user is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(user, exec);

		IdMappingState mappingState = config.getIdMapping().initToExternal(user, exec);
		
		// Get the locale extracted by the LocaleFilter
		PrintSettings settings = new PrintSettings(pretty, I18n.LOCALE.get(), datasetRegistry);
		Charset charset = determineCharset(userAgent, queryCharset);

		try {
			StreamingOutput out = exec.getResult(mappingState, settings, charset, config.getCsv().getLineSeparator());
			
			ResponseBuilder response = Response.ok(out);
			String label = exec.getLabel();
			if(!(Strings.isNullOrEmpty(label) || label.isBlank())) {
				// Set filename from label if the label was set, otherwise the browser will name the file according to the request path
				response.header("Content-Disposition", String.format("attachment; filename=\"%s.%s\"", exec.getLabel(), fileExtension));
			}
			return response;
		}
		catch (NoSuchElementException e) {
			throw new WebApplicationException(e, Status.NOT_FOUND);
		}
		finally {
			ConqueryMDC.clearLocation();
		}
	}

	/**
	 * Tries to determine the charset for the result encoding from different request properties.
	 * Defaults to StandardCharsets.UTF_8.
	 */
	private static Charset determineCharset(String userAgent, String queryCharset) {
		if(queryCharset != null) {
			try {
				return Charset.forName(queryCharset);				
			}catch (Exception e) {
				log.warn("Unable to map '{}' to a charset. Defaulting to UTF-8", queryCharset);
				return StandardCharsets.UTF_8;
			}
		}
		if(userAgent != null) {
			return userAgent.toLowerCase().contains("windows") ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
		}
		return StandardCharsets.UTF_8;
	}
}
