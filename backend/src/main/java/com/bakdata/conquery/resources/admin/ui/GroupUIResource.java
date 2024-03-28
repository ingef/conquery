package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static com.bakdata.conquery.resources.ResourceConstants.GROUPS_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

@Produces(MediaType.TEXT_HTML)
@Path(GROUPS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GroupUIResource {

	protected final UIProcessor uiProcessor;

	@GET
	public View getGroups() {
		return new UIView<>("groups.html.ftl", uiProcessor.getUIContext(), uiProcessor.getAdminProcessor().getAllGroups());
	}

	/**
	 * End point for retrieving information about a specific group.
	 *
	 * @param group Unique id of the group.
	 * @return A view holding the information about the group.
	 */
	@Path("{" + GROUP_ID + "}")
	@GET
	public View getGroup(@PathParam(GROUP_ID) Group group) {
		return new UIView<>("group.html.ftl", uiProcessor.getUIContext(), uiProcessor.getGroupContent(group));
	}
}