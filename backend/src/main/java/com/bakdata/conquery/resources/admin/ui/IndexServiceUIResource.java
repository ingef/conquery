package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.INDEX_SERVICE_PATH_ELEMENT;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path("/")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class IndexServiceUIResource {

	private final UIProcessor uiProcessor;

	@GET
	@Path(INDEX_SERVICE_PATH_ELEMENT)
	public View getScript() {
		return new UIView<>("indexService.html.ftl", uiProcessor.getUIContext(), uiProcessor.getIndexServiceStatistics());
	}
}
