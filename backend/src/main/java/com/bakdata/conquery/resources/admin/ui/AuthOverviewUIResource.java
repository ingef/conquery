package com.bakdata.conquery.resources.admin.ui;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path(ResourceConstants.AUTH_OVERVIEW_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AuthOverviewUIResource {

	protected final UIProcessor uiProcessor;

	@GET
	public View getOverview() {
		return new UIView<>("authOverview.html.ftl", uiProcessor.getUIContext(), uiProcessor.getAuthOverview());
	}

}
