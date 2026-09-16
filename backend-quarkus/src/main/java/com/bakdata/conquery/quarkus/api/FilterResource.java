package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/filters")
@Produces(MediaType.APPLICATION_JSON)
public class FilterResource {

	@Inject
	FilterService filterService;

	@POST
	@Path("/{filterId}/resolve")
	@Operation(
			summary = "Resolve uploaded filter values",
			description = "Resolves a list of raw values for a given filter id."
	)
	public FilterResolveResponse resolve(
			@PathParam("filterId") @NotBlank String filterId,
			@Valid @NotNull FilterValues payload
	) {
		FilterService.FilterResolveResult result = filterService.resolveFilterValues(filterId, payload.values);
		return new FilterResolveResponse(
				new ResolvedFilterResponse(
						result.resolvedFilter().tableId(),
						result.resolvedFilter().filterId(),
						result.resolvedFilter().value().stream()
							  .map(value -> new ResolvedFilterValueResponse(value.label(), value.value(), value.optionValue()))
							  .toList()
				),
				result.unknownCodes()
		);
	}

	@POST
	@Path("/{filterId}/autocomplete")
	@Operation(
			summary = "Autocomplete filter values",
			description = "Returns autocomplete suggestions for a filter."
	)
	public AutocompleteResponse autocomplete(
			@PathParam("filterId") @NotBlank String filterId,
			@Valid @NotNull AutocompleteRequest payload
	) {
		FilterService.FilterAutocompleteResult result = filterService.autocomplete(filterId, payload.text, payload.page, payload.pageSize);
		return new AutocompleteResponse(
				result.total(),
				result.values().stream()
					  .map(value -> new AutocompleteValueResponse(
							  value.label(),
							  value.value(),
							  value.optionValue(),
							  value.templateValues(),
							  value.disabled()
					  ))
					  .toList()
		);
	}

	public static final class FilterValues {
		public final @NotNull @NotEmpty List<@NotBlank String> values;

		public FilterValues(List<String> values) {
			this.values = values;
		}
	}

	public static final class AutocompleteRequest {
		public final @NotNull Optional<String> text;
		public final @NotNull OptionalInt page;
		public final @NotNull OptionalInt pageSize;

		public AutocompleteRequest(Optional<String> text, OptionalInt page, OptionalInt pageSize) {
			this.text = text;
			this.page = page;
			this.pageSize = pageSize;
		}
	}

	public record FilterResolveResponse(
			ResolvedFilterResponse resolvedFilter,
			List<String> unknownCodes
	) {
	}

	public record ResolvedFilterResponse(
			String tableId,
			String filterId,
			List<ResolvedFilterValueResponse> value
	) {
	}

	public record ResolvedFilterValueResponse(
			String label,
			String value,
			String optionValue
	) {
	}

	public record AutocompleteResponse(
			long total,
			List<AutocompleteValueResponse> values
	) {
	}

	public record AutocompleteValueResponse(
			String label,
			String value,
			String optionValue,
			java.util.Map<String, String> templateValues,
			boolean disabled
	) {
	}
}
