package com.bakdata.eva.forms.resources;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.URLBuilder.URLBuilderPath;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.eva.forms.common.FormAnswer;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.forms.managed.ManagedForm;
import com.bakdata.eva.forms.managed.ManagedStatisticForm;
import com.bakdata.eva.models.config.StatisticConfig;

import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Path("datasets/{" + DATASET + "}/result/")
@PermitAll
@Slf4j
public class StatisticResultResource {

	public static final URLBuilderPath DOWNLOAD_PATH = new URLBuilderPath(StatisticResultResource.class, "download");
	private FormProcessor processor;

	@GET
	@Path("{" + QUERY + "}.zip")
	public Response download(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId) throws MalformedURLException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		log.info("Querying results for {}", queryId);
		ManagedForm form = processor.getFormCache().get(queryId.getExecution());
		
		if(form == null) {
			throw new WebApplicationException("The query " + queryId + " is unknown", Status.NOT_FOUND);
		}
		FormAnswer result = ((ManagedStatisticForm)form).getResult();
		URL url =result.getUrl();
		URL statisticServer = ConqueryConfig.getInstance().getPluginConfig(StatisticConfig.class).getUrl();
		
		URL translatedURL = new URL(statisticServer.getProtocol(), statisticServer.getHost(), url.getPort(), url.getFile());
		
		try {
			log.info("Querying result from {}", translatedURL);
			BufferedInputStream input = new BufferedInputStream(translatedURL.openStream());
			return Response
				.ok(input, result.getMimeType())
				.build();
		}
		catch (Exception e) {
			String msg = "Statistic Form Execution failed";
			log.error(msg, e);
			throw new WebApplicationException(msg,e);
		}
	}
}
