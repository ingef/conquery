package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.GROUP_ID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HGroups;
import io.dropwizard.views.View;

@Produces(MediaType.TEXT_HTML)
public class GroupUIResource {


	@Inject
	protected AdminProcessor processor;
	@Inject
	protected UIProcessor uiProcessor;

	@GET
	public View getGroups() {
		return new UIView<>("groups.html.ftl", uiProcessor.getUIContext(), processor.getAllGroups());
	}

	/**
	 * End point for retrieving information about a specific group.
	 * 
	 * @param roleId
	 *            Unique id of the role.
	 * @return A view holding the information about the group.
	 */
	@Path("{" + GROUP_ID + "}")
	@GET
	public View getUser(@PathParam(GROUP_ID) Group group) {
		return new UIView<>("group.html.ftl", uiProcessor.getUIContext(), processor.getGroupContent(group));
	}
}