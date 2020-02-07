package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.externalservice.ResultType;
import lombok.Builder;
import lombok.Getter;

/**
 * Container class for the query API provide meta data for reach column in the
 * csv result. This can be used for the result preview in the frontend.
 */
@Getter
@Builder
public class ColumnDescriptor {

	private String label;
	private ResultType type;

}
