package com.bakdata.conquery.news;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;

@Path("news")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminNewsResource {

	private final NewsService newsService;

	@GET
	public List<NewsItem> getNews() {
		return newsService.getNews(null);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addNewsItem(@NotNull @Valid NewsItem newsItem) throws IOException {
		newsService.addNewsItem(newsItem);
		return Response.status(Response.Status.CREATED).entity(newsItem).build();
	}

	@DELETE
	@Path("{id}")
	public Response removeNewsItem(@PathParam("id") String id) throws IOException {
		if(!newsService.removeNewsItem(id)) {
			throw new NoSuchElementException("No news item with id '%s' exists".formatted(id));
		}

		return Response.noContent().build();
	}
}
