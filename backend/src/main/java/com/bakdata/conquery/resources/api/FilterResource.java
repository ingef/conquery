package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.resources.hierarchies.HFilters;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FilterResource extends HFilters {

	private final ConceptsProcessor processor;

	@POST
	@Path("resolve")
	public ResolvedConceptsResult resolveFilterValues(FilterValues filterValues) {
		return processor.resolveFilterValues((SelectFilter<?>) filter, filterValues.getValues());
	}

	@POST
	@Path("autocomplete")
	public ConceptsProcessor.AutoCompleteResult autocompleteTextFilter(@Valid FilterResource.AutocompleteRequest request) {

		if (!(filter instanceof SelectFilter)) {
			throw new WebApplicationException(filter.getId() + " is not a SELECT filter, but " + filter.getClass().getSimpleName() + ".", Status.BAD_REQUEST);
		}


		try {
			return processor.autocompleteTextFilter((SelectFilter<?>) filter, request.getText(), request.getPage(), request.getPageSize());
		}catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		}
	}

	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class FilterValues {
		private final List<String> values;
	}

	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class AutocompleteRequest {
		@NonNull
		private final Optional<String> text;
		@NonNull
		private final OptionalInt page;
		@NonNull
		private final OptionalInt pageSize;
	}
}
