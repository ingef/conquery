package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.ROLES_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path(ROLES_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoleUIResource {

	protected final UIProcessor uiProcessor;
	@Context
	private ContainerRequestContext requestContext;

	@GET
	public View getRoles() {
		return new UIView<>("roles.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getAdminProcessor()
																																			.getAllRoles()
		);
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
	public View getRole(@PathParam(ROLE_ID) RoleId role) {
		return new UIView<>("role.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getRoleContent(role));
	}
}
