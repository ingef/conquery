package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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
