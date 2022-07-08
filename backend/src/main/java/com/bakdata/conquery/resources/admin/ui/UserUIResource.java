package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.bakdata.conquery.resources.ResourceConstants.*;

@Produces(MediaType.TEXT_HTML)
@Path(USERS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UserUIResource {

	protected final UIProcessor uiProcessor;

	@GET
	public View getUsers() {
		return new UIView<>("users.html.ftl", uiProcessor.getUIContext(), uiProcessor.getAdminProcessor().getAllUsers());
	}

	/**
	 * End point for retrieving information about a specific user.
	 * 
	 * @param user Unique id of the user.
	 * @return A view holding the information about the user.
	 */
	@Path("{" + USER_ID + "}")
	@GET
	public View getUser(@PathParam(USER_ID) User user) {
		return new UIView<>("user.html.ftl", uiProcessor.getUIContext(), uiProcessor.getUserContent(user));
	}
}
