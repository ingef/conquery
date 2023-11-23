package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.INDEX_SERVICE_PATH_ELEMENT;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.index.IndexKey;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.google.common.cache.CacheStats;
import io.dropwizard.views.View;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path("/")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class IndexServiceUIResource {

	private final UIProcessor uiProcessor;

	@GET
	@Path(INDEX_SERVICE_PATH_ELEMENT)
	public View getIndexService() {
		final IndexServiceUIContent content = new IndexServiceUIContent(uiProcessor.getIndexServiceStatistics(), uiProcessor.getLoadedIndexes());
		return new UIView<>("indexService.html.ftl", uiProcessor.getUIContext(), content);
	}

	/**
	 * Data container for freemarker. Cannot use records yet: <a href="https://issues.apache.org/jira/browse/FREEMARKER-183">record issue</a>
	 */
	@Data
	public static class IndexServiceUIContent {
		private final CacheStats stats;
		private final Set<IndexKey<?>> indexes;
	}
}
