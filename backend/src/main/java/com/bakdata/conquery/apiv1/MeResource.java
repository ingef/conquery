package com.bakdata.conquery.apiv1;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.MeProcessor.FEGroup;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;

import lombok.Setter;

/**
 * This endpoint is used to query information about the user itself. The
 * endpoint might be used to ask the backend to which groups a query can be
 * shared.
 */
@Path("me")
@Setter
public class MeResource extends HAuthorized {

	@Inject
	private MeProcessor processor;

	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String myName() {
		return processor.getMyName(user);
	}

	@Path("groups")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public List<FEGroup> getMyGroups() {
		return processor.getMyGroups(user);
	}

}
