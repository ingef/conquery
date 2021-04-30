package com.bakdata.conquery.io.result;

import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.FileUtil;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ResultUtil {

	
	public static ExternalEntityId createId(Namespace namespace, EntityResult cer, IdMappingConfig idMappingConfig, IdMappingState mappingState) {
		EncodedDictionary dict = namespace.getStorage().getPrimaryDictionary();
		return idMappingConfig
			.toExternal(
				new CsvEntityId(dict.getElement(cer.getEntityId())),
				namespace,
				mappingState);
	}



	public static Response.ResponseBuilder makeResponseWithFileName(String fileExtension, ManagedExecution<?> exec, StreamingOutput out) {
		Response.ResponseBuilder response = Response.ok(out);
		String label = exec.getLabelWithoutAutoLabelSuffix();
		if(!(Strings.isNullOrEmpty(label) || label.isBlank())) {
			// Set filename from label if the label was set, otherwise the browser will name the file according to the request path
			response.header("Content-Disposition", String.format(
					"attachment; filename=\"%s.%s\"",
					FileUtil.SAVE_FILENAME_REPLACEMENT_MATCHER.matcher(label).replaceAll("_"),
					fileExtension));
		}
		return response;
	}
	/**
	 * Tries to determine the charset for the result encoding from different request properties.
	 * Defaults to StandardCharsets.UTF_8.
	 */
	public static Charset determineCharset(String userAgent, String queryCharset) {
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
