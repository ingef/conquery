package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotEmpty;
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
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.hierarchies.HGroups;

public class GroupResource extends HGroups {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Group> getGroups() {
		return processor.getAllGroups();
	}

	@Path("{" + GROUP_ID + "}")
	@DELETE
	public Response deleteGroup(@PathParam(GROUP_ID) Group group) throws JSONException {
		processor.deleteGroup(group);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postGroups(@NotEmpty List<Group> groups) throws JSONException {
		processor.addGroups(groups);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USER_PATH_ELEMENT + "/{" + USER_ID + "}")
	@POST
	public Response addUserToGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) throws JSONException {
		processor.addUserToGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USER_PATH_ELEMENT + "/{" + USER_ID + "}")
	@DELETE
	public Response deleteUserFromGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) throws JSONException {
		processor.deleteUserFromGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) throws JSONException {
		processor.deleteRoleFrom(group, role);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public Response addRoleToUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) throws JSONException {
		processor.addRoleTo(group, role);
		return Response.ok().build();
	}
}
