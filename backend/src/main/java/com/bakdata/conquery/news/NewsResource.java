package com.bakdata.conquery.news;


import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.auth.Auth;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Path("datasets/{" + DATASET + "}/news")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NewsResource extends HAuthorized {

    private final NewsService newsService;
	private final NewsPublicationPolicy publicationPolicy;
    @PathParam(DATASET)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
    private DatasetId datasetId;

    @GET
    public Collection<FrontendNewsItem> getNews(@Auth Subject subject) {
        subject.authorize(datasetId, Ability.READ);
        List<NewsItem> news = newsService.getNews(datasetId.getName());
		return news.stream()
				.filter(publicationPolicy::isPublished)
				.map(item ->
            new FrontendNewsItem(
                    item.id(),
                    item.title(),
                    item.description(),
                    item.link(),
                    item.date()
            )
				)
				.toList();
    }
}
