package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.models.auth.entities.Role;
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

import static com.bakdata.conquery.resources.ResourceConstants.ROLES_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;

@Produces(MediaType.TEXT_HTML)
@Path(ROLES_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoleUIResource {

	protected final UIProcessor uiProcessor;

	@GET
	public View getRoles() {
		return new UIView<>("roles.html.ftl", uiProcessor.getAdminProcessor().getAllRoles());
	}

	/**
	 * End point for retrieving information about a specific role.
	 *
	 * @param role
	 *            Unique id of the role.
	 * @return A view holding the information about the role.
	 */
	@Path("{" + ROLE_ID + "}")
	@GET
	public View getRole(@PathParam(ROLE_ID) Role role) {
		return new UIView<>("role.html.ftl", uiProcessor.getRoleContent(role));
	}
}
