package com.bakdata.conquery.apiv1.frontend;

import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.types.ResultType;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class FrontendResultType {
	@NotNull
	String type;

	@SuperBuilder
	public static class List extends FrontendResultType {
		@NotNull
		FrontendResultType elementType;
	}

	public static FrontendResultType from(ResultType resultType) {
		if (resultType instanceof ResultType.ListT<?> listT) {
			return List.builder()
					   .elementType(from(listT.getElementType()))
					   .type(listT.typeInfo()).build();
		}

		return FrontendResultType.builder().type(resultType.typeInfo()).build();
	}
}
