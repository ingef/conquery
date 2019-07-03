package com.bakdata.eva.forms.managed;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import com.bakdata.conquery.apiv1.ResourceConstants;
import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.eva.forms.common.FormAnswer;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.forms.resources.StatisticResultResource;
import com.bakdata.eva.models.config.StatisticConfig;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Getter
@Setter
@Slf4j
@ToString
@CPSType(base = ManagedExecution.class, id = "MANAGED_STATISTIC_FORM")
public class ManagedStatisticForm extends ManagedForm {

	private static final ContentType JSON_CT = ContentType.create("application/json", "utf-8");
	private static final ContentType CSV_CT = ContentType.create("text/csv", "utf-8");
	private static final ObjectReader ANSWER_READER = Jackson.MAPPER.readerFor(FormAnswer.class);
	
	private FormAnswer result;
	
	public ManagedStatisticForm(StatisticForm form, Namespace namespace, UserId owner) {
		super(form, namespace, owner);
	}

	@Override
	public ExecutionStatus buildStatus(URLBuilder url) {
		ExecutionStatus status = super.buildStatus(null);
		if(url != null) {
			//different result path since statisticforms pull files from a foreign server
			status.setResultUrl(
				url
					.set(ResourceConstants.DATASET, getDataset().getName())
					.set(ResourceConstants.QUERY, getId().toString())
					.to(StatisticResultResource.DOWNLOAD_PATH)
					.get()
			);
		}
		return status;
	}
	
	@Override
	protected ExecutionState execute() throws JSONException, IOException {
		ExecutionState partA = super.execute();
		if(partA != ExecutionState.DONE) {
			return partA;
		}
		
		ConqueryConfig config = ConqueryConfig.getInstance();
		String json = ((StatisticForm)form).toStatisticJSON(namespace.getNamespaces());

		QueryToCSVRenderer renderer = new QueryToCSVRenderer(namespace);
		PrintSettings settings = PrintSettings.builder().prettyPrint(false).nameExtractor(form.getColumnNamer().getNamer()).build();
		Stream<String> queryResult = renderer
			.toCSV(settings, internalQueries);
		
		ByteArrayOutputStream matrixOut = new ByteArrayOutputStream();
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(matrixOut, StandardCharsets.UTF_8))) {
			Iterator<String> it = queryResult.iterator();
			while (it.hasNext()) {
				writer.write(it.next());
				writer.write(config.getCsv().getLineSeparator());
			}
			writer.flush();
		}

		byte[] matrix = matrixOut.toByteArray();
		matrixOut = null;

		try {
			HttpResponse<String> response = Unirest
				.post(config.getPluginConfig(StatisticConfig.class).getUrl().toString())
				.header("accept", ExtraMimeTypes.JSON_STRING)
				.field("description", IOUtils.toInputStream(json, StandardCharsets.UTF_8), JSON_CT, "description.json")
				.field("matrix", new ByteArrayInputStream(matrix), CSV_CT, "matrix.csv")
				.asString();
			
			String answer = response.getBody();
			if (response.getStatus() > 299) {
				log.error("Respone of R-End for {}: {}({}): {}", getId(), response.getStatus(), response.getStatusText(), answer);
				fail();
			}
			else {
				log.debug("Respone of R-End for {}: {}({}): {}", getId(), response.getStatus(), response.getStatusText(), answer);
				result = ANSWER_READER.readValue(answer);
				finish();
				return ExecutionState.DONE;
			}
		}
		catch (UnirestException e) {
			log.error("Failed request to statistic server for "+getId(), e);
			fail();
		}
		return ExecutionState.FAILED;
	}
}
