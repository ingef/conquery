package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_NAME;


import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.hierarchies.HUsers;

public class UserResource extends HUsers{


	@POST
	public Response postUser(User user) throws JSONException {
		processor.addUser(user);
		return Response.ok().build();
	}
	
	@Path("{" + USER_ID + "}")
	@DELETE
	public Response deleteUser(@PathParam(USER_ID) UserId userId) throws JSONException {
		processor.deleteUser(userId);
		return Response.ok().build();
	}
	
	@Path("{" + USER_ID + "}/{" + ROLE_NAME + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(USER_ID) UserId userId, @PathParam(ROLE_NAME) RoleId roleId) throws JSONException {
		processor.deleteRoleFromUser(userId, roleId);
		return Response.ok().build();
	}
	
	@Path("{" + USER_ID + "}/{" + ROLE_NAME + "}")
	@POST
	public Response addRoleToUser(@PathParam(USER_ID) UserId userId, @PathParam(ROLE_NAME) RoleId roleId) throws JSONException {
		processor.addRoleToUser(userId, roleId);
		return Response.ok().build();
	}
}
