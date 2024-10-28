package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.USERS_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path(USERS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UserUIResource {

	protected final UIProcessor uiProcessor;
	@Context
	private ContainerRequestContext requestContext;

	@GET
	public View getUsers() {
		return new UIView("users.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getAdminProcessor()
																																		  .getAllUsers());
	}

	/**
	 * End point for retrieving information about a specific user.
	 * 
	 * @param user Unique id of the user.
	 * @return A view holding the information about the user.
	 */
	@Path("{" + USER_ID + "}")
	@GET
	public View getUser(@PathParam(USER_ID) UserId user) {
		return new UIView("user.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getUserContent(user));
	}
}
