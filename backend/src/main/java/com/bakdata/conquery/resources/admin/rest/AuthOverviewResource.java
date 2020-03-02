package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.hierarchies.HAuthOverview;

/**
 * This class provides endpoints to obtain an authorization overview in form of structured data.
 */
@Produces(AdditionalMediaTypes.CSV)
public class AuthOverviewResource extends HAuthOverview {

	@GET
	public Response getPermissionOverviewAsCSV() {
		return Response.ok(processor.getPermissionOverviewAsCSV()).build();
	}

	@GET
	@Path("group/{"+ GROUP_ID +"}")
	public Response getPermissionOverviewAsCSV(@PathParam(value = GROUP_ID) GroupId groupId) {
		return Response.ok(processor.getPermissionOverviewAsCSV(groupId)).build();
	}
}
