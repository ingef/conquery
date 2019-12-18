package com.bakdata.conquery.resources.admin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.resources.hierarchies.HAdmin;


@Path("auth-overview")
public class AuthOverviewResource extends HAdmin {

	@GET
	@Produces(AdditionalMediaTypes.CSV)
	public String getPermissionOverviewAsCSV() {
		return processor.getPermissionOverviewAsCSV();
	}

}
