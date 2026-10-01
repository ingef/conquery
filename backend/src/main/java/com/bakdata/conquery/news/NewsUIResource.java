package com.bakdata.conquery.news;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Path("news")
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NewsUIResource {

	private final UIProcessor uiProcessor;

	@Context
	private ContainerRequestContext requestContext;

	@GET
	public View getNews() {
		List<String> datasetIds = uiProcessor.getDatasetRegistry()
				.getAllDatasets()
				.map(DatasetId::getName)
				.sorted()
				.toList();
		return new UIView<>(
				"news.html.ftl",
				uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)),
				new NewsUIModel(datasetIds)
		);
	}

	@Getter
	@RequiredArgsConstructor
	public static class NewsUIModel {
		private final List<String> datasetIds;
	}
}
