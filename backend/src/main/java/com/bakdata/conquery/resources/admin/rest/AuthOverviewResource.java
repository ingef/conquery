package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.io.FileUtil;
import lombok.RequiredArgsConstructor;

/**
 * This class provides endpoints to obtain an authorization overview in form of structured data.
 */
@Produces(AdditionalMediaTypes.CSV)
@Path(ResourceConstants.AUTH_OVERVIEW_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AuthOverviewResource {

	private final AdminProcessor processor;

	@GET
	@Path("csv")
	public Response getPermissionOverviewAsCSV() {
		return Response
				.ok(processor.getPermissionOverviewAsCSV())
				.header("Content-Disposition", "attachment; filename=\"authOverview.csv\"")
				.build();
	}

	@GET
	@Path("csv/group/{" + GROUP_ID + "}")
	public Response getPermissionOverviewAsCSV(@PathParam(GROUP_ID) Group group) {
		return Response
				.ok(processor.getPermissionOverviewAsCSV(group))
				.header("Content-Disposition", String.format("attachment; filename=\"authOverview_%s.csv\"", FileUtil.makeSafeFileName(group.getName(), "csv")))
				.build();
	}
}
