package com.bakdata.conquery.apiv1.query;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


/**
 * Data Container for API call of {@link com.bakdata.conquery.resources.api.QueryResource#upload(User, ExternalUpload)}.
 *
 * This class acts as a wrapper for {@link com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter(AccessLevel.PRIVATE)
public class ExternalUpload {

	private String label;
	private List<String> format;
	private String[][] values;
	private boolean oneRowPerEntity;

	@JsonIgnore
	@ValidationMethod(message = "Values and Format are not of same width.")
	public boolean isAllSameLength() {
		final int expected = format.size();
		return Arrays.stream(values).mapToInt(a -> a.length).allMatch(v -> expected == v);
	}
}
