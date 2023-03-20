package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.FILTER;

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

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
// It allows for resolving more than filters but that's not interesting from an API perspective so we keep it at /filters/
@Path("filters/{" + FILTER + "}/autocomplete")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@ToString
public class AutocompleteResource extends HAuthorized {
	@ToString.Exclude
	private final ConceptsProcessor processor;

	@PathParam(FILTER)
	protected Searchable<?> searchable;

	@POST
	public ConceptsProcessor.AutoCompleteResult autocompleteTextFilter(@Valid AutocompleteRequest request) {
		subject.isPermitted(searchable.getDataset(), Ability.READ);


		if (searchable instanceof SelectFilter selectFilter) {
			subject.isPermitted(selectFilter.getConnector().findConcept(), Ability.READ);
		}


		try {
			return processor.autocompleteTextFilter(searchable, request.text(), request.page(), request.pageSize());
		}
		catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
	}

	public record AutocompleteRequest(@NonNull Optional<String> text, @NonNull OptionalInt page, @NonNull OptionalInt pageSize) {
	}
}
