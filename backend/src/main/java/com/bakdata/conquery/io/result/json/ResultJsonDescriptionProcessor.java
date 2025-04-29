package com.bakdata.conquery.io.result.json;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultJsonDescriptionProcessor {


	public <E extends ManagedQuery> Response createResult(Subject subject, E exec, Charset charset) {


		ConqueryMDC.setLocation(subject.getName());
		log.info("Downloading JSON Description for {}", exec.getId());

		ResultUtil.authorizeExecutable(subject, exec);

		final StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset))) {
				Jackson.MAPPER.copy()
							  .writerWithDefaultPrettyPrinter()
							  .forType(QueryDescription.class)
							  .writeValue(writer, exec.getQuery());
			}
			catch (EofException e) {
				log.trace("User canceled download");
			}
			catch (Exception e) {
				throw new WebApplicationException("Failed to load result", e);
			}
			finally {
				log.trace("FINISHED downloading {}", exec.getId());
			}
		};

		return makeResponseWithFileName(
				Response.ok(out),
				String.join(".", exec.getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_JSON),
				MediaType.APPLICATION_JSON_TYPE,
				ResultUtil.ContentDispositionOption.ATTACHMENT
		);

	}
}
