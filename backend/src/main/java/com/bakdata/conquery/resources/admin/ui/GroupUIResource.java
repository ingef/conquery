package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.GROUPS_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path(GROUPS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GroupUIResource {

	protected final UIProcessor uiProcessor;
	@Context
	private ContainerRequestContext requestContext;

	@GET
	public View getGroups() {
		return new UIView<>("groups.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getAdminProcessor()
																																			 .getAllGroups()
		);
	}

	/**
	 * End point for retrieving information about a specific group.
	 *
	 * @param group Unique id of the group.
	 * @return A view holding the information about the group.
	 */
	@Path("{" + GROUP_ID + "}")
	@GET
	public View getGroup(@PathParam(GROUP_ID) GroupId group) {
		return new UIView<>("group.html.ftl",
							uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)),
							uiProcessor.getGroupContent(group.resolve())
		);
	}
}