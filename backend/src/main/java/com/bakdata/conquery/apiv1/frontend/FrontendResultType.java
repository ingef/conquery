package com.bakdata.conquery.apiv1.frontend;

import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class FrontendResultType {
	@NotNull
	String type;

	@Getter
	@SuperBuilder
	public static class List extends FrontendResultType {
		@NotNull
		FrontendResultType elementType;
	}

	public static FrontendResultType from(ResultType resultType) {
		if (resultType instanceof ResultType.ListT<?> listT) {
			return List.builder()
					   .elementType(from(listT.getElementType()))
					   .type("LIST").build();
		}

		return FrontendResultType.builder().type(resultType.typeInfo()).build();
	}
}
