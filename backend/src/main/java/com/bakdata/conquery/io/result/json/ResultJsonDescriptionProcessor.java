package com.bakdata.conquery.io.result.json;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.EofException;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultJsonDescriptionProcessor {


	public Response createResult(Subject subject, ManagedExecutionId execId) {

		ManagedExecution exec = execId.resolve();

		ConqueryMDC.setLocation(subject.getName());
		log.debug("Downloading JSON Description for {}", execId);

		ResultUtil.authorizeExecutable(subject, exec);

		final StreamingOutput out = os -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
				Jackson.MAPPER.copy()
							  .writerWithDefaultPrettyPrinter()
							  .forType(QueryDescription.class)
							  .writeValue(writer, exec.getSubmitted());
			}
			catch (EofException e) {
				log.trace("User canceled download");
			}
			catch (Exception e) {
				throw new InternalServerErrorException("Failed to load result", e);
			}
			finally {
				log.trace("FINISHED downloading {}", execId);
			}
		};

		return makeResponseWithFileName(Response.ok(out),
										String.join(".", exec.getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_JSON),
										MediaType.APPLICATION_JSON_TYPE,
										ResultUtil.ContentDispositionOption.ATTACHMENT
		);

	}

}
