package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;
import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.hierarchies.HGroups;

public class GroupResource extends HGroups{


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Group> getGroups() {
		return processor.getAllGroups();
	}

	@Path("{" + GROUP_ID + "}")
	@DELETE
	public Response deleteGroup(@PathParam(GROUP_ID) GroupId groupId) throws JSONException {
		processor.removeGroup(groupId);
		return Response.ok().build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postGroups(List<Group> groups) throws JSONException {
		processor.addGroups(groups);
		return Response.ok().build();
	}
	
	@Path("{" + GROUP_ID + "}/{" + USER_ID + "}")
	@POST
	public Response addUserToGroup(@PathParam(USER_ID) UserId userId, @PathParam(GROUP_ID) GroupId groupId) throws JSONException {
		processor.addUserToGroup(groupId, userId);
		return Response.ok().build();
	}
	
	@Path("{" + GROUP_ID + "}/{" + USER_ID + "}")
	@DELETE
	public Response deleteUserFromGroup(@PathParam(USER_ID) UserId userId, @PathParam(GROUP_ID) GroupId groupId) throws JSONException {
		processor.deleteUserFromGroup(groupId, userId);
		return Response.ok().build();
	}
}
