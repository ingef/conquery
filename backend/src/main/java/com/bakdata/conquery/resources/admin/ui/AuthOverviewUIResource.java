package com.bakdata.conquery.resources.admin.ui;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;

import java.util.Collections;

@Produces(MediaType.TEXT_HTML)
@Path(ResourceConstants.AUTH_OVERVIEW_PATH_ELEMENT)
public class AuthOverviewUIResource {
	@Inject
	protected UIProcessor uiProcessor;

	@GET
	public View getOverview() {
		return new UIView<>("authOverview.html.ftl", uiProcessor.getUIContext(), uiProcessor.getAuthOverview());
	}

}
