package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HUsers;
import io.dropwizard.views.View;

import java.util.Collections;

@Produces(MediaType.TEXT_HTML)
public class UserUIResource {

	@Inject
	protected UIProcessor uiProcessor;

	@GET
	public View getUsers() {
		return new UIView<>("users.html.ftl", uiProcessor.getUIContext(), Collections.emptyList());
	}

	/**
	 * End point for retrieving information about a specific role.
	 * 
	 * @param roleId
	 *            Unique id of the role.
	 * @param user
	 * @return A view holding the information about the role.
	 */
	@Path("{" + USER_ID + "}")
	@GET
	public View getUser(@PathParam(USER_ID) User user) {
		return new UIView<>("user.html.ftl", uiProcessor.getUIContext(), Collections.emptyMap());
	}
}
