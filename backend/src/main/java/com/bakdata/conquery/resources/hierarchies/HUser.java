package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import lombok.Setter;

@Setter
@Path("users/{" + USER_ID + "}")
public abstract class HUser extends HAuthorized {

	@Inject
	protected AdminProcessor processor;
	@PathParam(USER_ID)
	protected UserId addressedUserId;
	protected User addressedUser;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		addressedUser = processor.getStorage().getUser(addressedUserId);
		if(addressedUser == null) {
			throw new WebApplicationException(String.format("Could not find user with ID: {}", addressedUserId), Status.NOT_FOUND);
		}
		
	}

}
