package com.bakdata.conquery.apiv1;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Slf4j
@Path("me")
@Setter
public class MeResource extends HAuthorized {

	@Inject
	private MeProcessor processor;

	@Path("groups")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public List<GroupId> getMyGroups(){
		return processor.getMyGroups(user);
	}

}
