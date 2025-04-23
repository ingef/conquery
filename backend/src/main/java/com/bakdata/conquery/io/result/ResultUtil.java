package com.bakdata.conquery.io.result;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeDownloadDatasets;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.util.io.FileUtil;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultUtil {

	private static final Charset WINDOWS_DEFAULT_CHARSET = Charset.forName("windows-1252");

	public enum ContentDispositionOption {
		// Try to display payload in the browser
		INLINE,
		// Force download of the payload by the browser
		ATTACHMENT;

		String getHeaderValue() {
			return name().toLowerCase(Locale.ROOT);
		}
	}


	public static Response makeResponseWithFileName(Response.ResponseBuilder response, String filename, MediaType mediaType, ContentDispositionOption disposition) {
		response.header(HttpHeaders.CONTENT_TYPE, mediaType);
		if (!(Strings.isNullOrEmpty(filename) || filename.isBlank())) {
			// Set filename from filename if the filename was set, otherwise the browser will name the file according to the request path
			response.header("Content-Disposition", String.format(
					"%s; filename=\"%s\"", disposition.getHeaderValue(), FileUtil.makeSafeFileName(filename)));
		}
		return response.build();
	}

	/**
	 * Tries to determine the charset for the result encoding from different request properties.
	 * Defaults to StandardCharsets.UTF_8.
	 */
	public static Charset determineCharset(String userAgent, String queryCharset) {
		if (queryCharset != null) {
			try {
				return Charset.forName(queryCharset);
			}
			catch (Exception e) {
				log.warn("Unable to map '{}' to a charset.", queryCharset);
			}
		}
		if (userAgent != null && userAgent.toLowerCase().contains("windows")) {
			return WINDOWS_DEFAULT_CHARSET;
		}
		return StandardCharsets.UTF_8;
	}


	/**
	 * Throws a "Bad Request" response if the execution result is not a single table.
	 *
	 * @param exec the execution to test
	 */
	public static void checkSingleTableResult(ManagedExecution exec) {
		if (!(exec instanceof SingleTableResult)) {
			throw new BadRequestException("Execution cannot be rendered as the requested format");
		}
	}


	public static void authorizeExecutable(Subject subject, ManagedExecution exec) {
		final DatasetId datasetId = exec.getDataset();
		subject.authorize(datasetId, Ability.READ);
		subject.authorize(datasetId, Ability.DOWNLOAD);


		subject.authorize(exec, Ability.READ);

		// Check if subject is permitted to download on all datasets that were referenced by the query
		authorizeDownloadDatasets(subject, exec);
	}

}
