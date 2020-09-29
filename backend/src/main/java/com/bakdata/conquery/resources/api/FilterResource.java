package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.FILTER;
import static com.bakdata.conquery.resources.ResourceConstants.MAX_RESULT_COUNT;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE;

import java.util.List;
import java.util.OptionalInt;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.resources.hierarchies.HFilters;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}")
public class FilterResource extends HFilters {
	
	@Inject
	protected ConceptsProcessor processor;

	@POST
	@Path("resolve")
	public ResolvedConceptsResult resolveFilterValues(FilterValues filterValues) {
		return processor.resolveFilterValues((AbstractSelectFilter<?>)filter, filterValues.getValues());
	}
	
	@POST
	@Path("autocomplete")
	public List<FEValue> autocompleteTextFilter(@NotNull StringContainer text, @PathParam(MAX_RESULT_COUNT) OptionalInt maxResultNumber) {
		if(StringUtils.isEmpty(text.getText())) {
			throw new WebApplicationException("Too short text. Requires at least 1 characters.", Status.BAD_REQUEST);
		}
		if(!(filter instanceof AbstractSelectFilter)) {
			throw new WebApplicationException(filter.getId()+" is not a SELECT filter, but "+filter.getClass().getSimpleName()+".", Status.BAD_REQUEST);
		}
		

		return processor.autocompleteTextFilter((AbstractSelectFilter<?>) filter, text.getText(), maxResultNumber);
	}
	
	@Data
	public static class FilterValues {
		private List<String> values;
	}
	
	@Data
	public static class StringContainer {
		private String text;
	}
}
