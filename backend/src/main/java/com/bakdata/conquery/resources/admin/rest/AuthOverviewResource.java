package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
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
				.header("Content-Disposition", String.format("attachment; filename=\"authOverview_%s.csv\"", FileUtil.makeSafeFileName(group.getName())))
				.build();
	}
}
