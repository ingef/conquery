package com.bakdata.conquery.models.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import org.junit.jupiter.api.Test;

class ResultTypeTest {

	@Test
	void shouldResolveMajorTypesExplicitly() {
		Map<MajorTypeId, ResultType> expected = Map.of(
				MajorTypeId.STRING, ResultType.Primitive.STRING,
				MajorTypeId.BOOLEAN, ResultType.Primitive.BOOLEAN,
				MajorTypeId.DATE, ResultType.Primitive.DATE,
				MajorTypeId.DATE_RANGE, ResultType.Primitive.DATE_RANGE,
				MajorTypeId.INTEGER, ResultType.Primitive.INTEGER,
				MajorTypeId.MONEY, ResultType.Primitive.MONEY,
				MajorTypeId.DECIMAL, ResultType.Primitive.NUMERIC,
				MajorTypeId.REAL, ResultType.Primitive.NUMERIC
		);

		expected.forEach((majorType, resultType) -> assertEquals(resultType, ResultType.resolveResultType(majorType)));
	}

	@Test
	void shouldDescribePrimitiveAndListTypes() {
		assertEquals("STRING", ResultType.Primitive.STRING.typeInfo());
		assertEquals("LIST[DATE_RANGE]", new ResultType.ListT(ResultType.Primitive.DATE_RANGE).typeInfo());
		assertEquals(
				"LIST[LIST[STRING]]",
				new ResultType.ListT(new ResultType.ListT(ResultType.Primitive.STRING)).typeInfo()
		);
	}

	@Test
	void shouldCompareListTypesByElementType() {
		ResultType stringList = new ResultType.ListT(ResultType.Primitive.STRING);

		assertEquals(stringList, new ResultType.ListT(ResultType.Primitive.STRING));
		assertEquals(stringList.hashCode(), new ResultType.ListT(ResultType.Primitive.STRING).hashCode());
		assertNotEquals(stringList, new ResultType.ListT(ResultType.Primitive.INTEGER));
	}

	@Test
	void shouldRejectNullListElementType() {
		assertThrows(NullPointerException.class, () -> new ResultType.ListT(null));
	}
}
