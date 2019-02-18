package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.io.IOException;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespaces;

import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;

@Path("datasets/{" + DATASET + "}/import")
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
@AllArgsConstructor
public class ImportResource {

	private final Namespaces namespaces;

	@Consumes(AdditionalMediaTypes.CSV)
	@POST
	public SQStatus postConstantQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull String input, @Context HttpServletRequest req) throws IOException {
		authorize(user, datasetId, Ability.READ);

		Dataset dataset = namespaces.get(datasetId).getStorage().getDataset();
		// Wait for new CSV Handler see https://github.com/bakdata/conquery/issues/243
		// What is the new Version of a ConstantQuery see
		// https://github.com/bakdata/conquery/issues/242
		/*
		 * ConstantQuery cq =
		 * ConstantQuery.of(queryProcessor.getConfig().getResultIdMapper(), input,
		 * version); return queryProcessor.postQuery(user, dataset.get(datasets), cq,
		 * URLBuilder.fromRequest(req));
		 */
		return null;
	}
}
