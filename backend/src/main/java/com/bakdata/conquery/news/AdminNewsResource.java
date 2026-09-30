package com.bakdata.conquery.news;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;

@Path("news")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminNewsResource {

	private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");
	// See about:blank https://www.rfc-editor.org/rfc/rfc9457.html#section-4.2.1
	private static final URI ABOUT_BLANK = URI.create("about:blank");
	private final NewsService newsService;

	@GET
	public List<NewsItem> getNews() {
		return newsService.getNews(null);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addNewsItem(NewsItem newsItem, @Context UriInfo uriInfo) throws IOException {
		if(newsItem == null) {
			return problem(422, "Unprocessable Entity", "News item must not be null", uriInfo);
		}

		try {
			newsService.addNewsItem(newsItem);
			return Response.status(Response.Status.CREATED).entity(newsItem).build();
		}
		catch(ConstraintViolationException exception) {
			String detail = exception.getConstraintViolations().stream()
					.map(AdminNewsResource::describeViolation)
					.sorted()
					.reduce((left, right) -> left + " AND " + right)
					.orElse("News item is invalid");
			return problem(422, "Unprocessable Entity", detail, uriInfo);
		}
		catch(IllegalArgumentException exception) {
			return problem(
					Response.Status.CONFLICT.getStatusCode(),
					Response.Status.CONFLICT.getReasonPhrase(),
					exception.getMessage(),
					uriInfo
			);
		}
	}

	@DELETE
	@Path("{id}")
	public Response removeNewsItem(@PathParam("id") String id, @Context UriInfo uriInfo) throws IOException {
		if(newsService.removeNewsItem(id)) {
			return Response.noContent().build();
		}

		return problem(
				Response.Status.NOT_FOUND.getStatusCode(),
				Response.Status.NOT_FOUND.getReasonPhrase(),
				"No news item with id '%s' exists".formatted(id),
				uriInfo
		);
	}

	private static String describeViolation(ConstraintViolation<?> violation) {
		return violation.getPropertyPath() + " " + violation.getMessage();
	}

	// We adapt to RFC 9457 here. There are libraries we could use (e.g. org.zalando:problem) in jersey/dropwizard,
	// but quarkus has better integrations for this.
	private static Response problem(int status, String title, String detail, UriInfo uriInfo) {
		ProblemDetails problem = new ProblemDetails(ABOUT_BLANK, title, status, detail, uriInfo.getRequestUri());
		return Response.status(status).type(PROBLEM_JSON).entity(problem).build();
	}

	public record ProblemDetails(URI type, String title, int status, String detail, URI instance) {
	}
}
