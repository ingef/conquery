package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.FILTER;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
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
	protected FilterId filter;

	@POST
	@Path("resolve")
	public ConceptsProcessor.ResolvedFilterValues resolveFilterValues(FilterValues filterValues) {
		subject.isPermitted(filter.getDataset(), Ability.READ);
		subject.isPermitted(filter.getConnector().getConcept(), Ability.READ);

		return processor.resolveFilterValues(filter, filterValues.values());
	}

	//TODO migrate from filter to searchable
	@POST
	@Path("autocomplete")
	public ConceptsProcessor.AutoCompleteResult autocompleteTextFilter(@Valid FilterResource.AutocompleteRequest request) {
		subject.isPermitted(filter.getDataset(), Ability.READ);
		subject.isPermitted(filter.getConnector().getConcept(), Ability.READ);

		if (!(filter.resolve() instanceof SelectFilter)) {
			throw new WebApplicationException(filter + " is not a SELECT filter, but " + filter.getClass().getSimpleName() + ".", Status.BAD_REQUEST);
		}


		try {
			return processor.autocompleteTextFilter(filter, request.text().orElse(null), request.page(), request.pageSize());
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
