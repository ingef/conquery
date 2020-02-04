package com.bakdata.conquery.models.auth.basic.web;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.ProtoUser;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;

@Path("local-authentiaction")
public class UserAuthenticationManagementResource extends HAuthorized{
	
	@Inject
	private UserAuthenticationManagementProcessor processor;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUser(ProtoUser pUser) {
		
		if(processor.addUser(pUser)) {			
			return Response.status(Status.CREATED).build();
		}
		return Response.serverError().status(Status.CONFLICT).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUser(ProtoUser pUser) {
		
		if(processor.updateUser(pUser)) {			
			return Response.status(Status.CREATED).build();
		}
		return Response.serverError().status(Status.CONFLICT).build();
	}

	@Path("{" + USER_ID + "}")
	@DELETE
	public Response removeUser(@PathParam(USER_ID) UserId userId) {
		processor.remove(userId);
		return Response.ok().build();
	
	}

}
