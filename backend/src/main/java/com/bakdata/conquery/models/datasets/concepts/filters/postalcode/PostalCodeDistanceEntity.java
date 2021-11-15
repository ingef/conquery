package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.univocity.parsers.annotations.Parsed;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.NonNull;

@Data
public class PostalCodeDistanceEntity {

	@Parsed
	@NonNull
	@NotEmpty
	@Size(min = 5, max = 5)
	final private String plz1;

	@Parsed
	@NonNull
	@NotEmpty
	@Size(min = 5, max = 5)
	final private String plz2;

	@Parsed
	@Min(0)
	final private double distance;


	@ValidationMethod(message = "The postal codes can not be equals")
	public boolean isPostalCodesDifferent() {
		return !plz1.equals(plz2);
	}
}
