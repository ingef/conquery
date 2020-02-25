package com.bakdata.conquery.resources.admin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.resources.hierarchies.HAdmin;

/**
 * This class provides endpoints to obtain an authorization overview in form of structured data.
 */
@Path("auth-overview")
public class AuthOverviewResource extends HAdmin {

	@GET
	@Produces(AdditionalMediaTypes.CSV)
	public String getPermissionOverviewAsCSV() {
		return processor.getPermissionOverviewAsCSV();
	}

}
