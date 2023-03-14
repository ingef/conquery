package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.FILTER;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("filters/{" + FILTER + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@ToString(onlyExplicitlyIncluded = true)
public class FilterResource extends HAuthorized {

	private final ConceptsProcessor processor;

	@ToString.Include
	@PathParam(FILTER)
	protected Filter<?> filter;

	@POST
	@Path("resolve")
	public ConceptsProcessor.ResolvedFilterValues resolveFilterValues(FilterValues filterValues) {
		subject.isPermitted(filter.getDataset(), Ability.READ);
		subject.isPermitted(filter.getConnector().findConcept(), Ability.READ);

		return processor.resolveFilterValues((SelectFilter<?>) filter, filterValues.values());
	}

	//TODO migrate from filter to searchable
	@POST
	@Path("autocomplete")
	public ConceptsProcessor.AutoCompleteResult autocompleteTextFilter(@Valid FilterResource.AutocompleteRequest request) {
		subject.isPermitted(filter.getDataset(), Ability.READ);
		subject.isPermitted(filter.getConnector().findConcept(), Ability.READ);

		if (!(filter instanceof SelectFilter)) {
			throw new WebApplicationException(filter.getId() + " is not a SELECT filter, but " + filter.getClass().getSimpleName() + ".", Status.BAD_REQUEST);
		}


		try {
			return processor.autocompleteTextFilter((SelectFilter<?>) filter, request.text(), request.page(), request.pageSize());
		}
		catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
	}

	public record FilterValues(List<String> values) {
	}

	public record AutocompleteRequest(@NonNull Optional<String> text, @NonNull OptionalInt page, @NonNull OptionalInt pageSize) {
	}
}
