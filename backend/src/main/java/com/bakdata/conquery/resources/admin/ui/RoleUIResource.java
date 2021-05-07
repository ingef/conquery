package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.ROLES_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;

@Produces(MediaType.TEXT_HTML)
@Path(ROLES_PATH_ELEMENT)
public class RoleUIResource extends HAdmin {

	@Inject
	protected AdminProcessor processor;
	@Inject
	protected UIProcessor uiProcessor;

	@GET
	public View getRoles() {
		return new UIView<>("roles.html.ftl", uiProcessor.getUIContext(), processor.getAllRoles());
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
		return new UIView<>("role.html.ftl", uiProcessor.getUIContext(), processor.getRoleContent(role));
	}
}
