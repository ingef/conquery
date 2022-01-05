package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import lombok.Data;

@Data
public class PostalCodeSearchEntity {


	@Size(min = 4, max = 5)
	@NotNull
	private String plz;

	@Min(0)
	private double radius;

	@InternalOnly
	private String[] resolvedValue;

	@JacksonInject
	@JsonIgnore
	private PostalCodesManager postalCodesManager;

	public void resolve(QueryResolveContext context) {
		Preconditions.checkNotNull(postalCodesManager);
		resolvedValue = postalCodesManager.filterAllNeighbours(Integer.parseInt(getPlz()), getRadius());

	}
}
