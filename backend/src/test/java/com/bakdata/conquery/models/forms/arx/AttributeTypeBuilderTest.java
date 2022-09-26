package com.bakdata.conquery.models.forms.arx;

import static org.assertj.core.api.Assertions.*;

import org.deidentifier.arx.DataType;
import org.junit.jupiter.api.Test;

public class AttributeTypeBuilderTest {

	@Test
	public void integerIntervalBuilder() {
		final AttributeTypeBuilder.IntegerInterval builder = new AttributeTypeBuilder.IntegerInterval();

		builder.register(0L);
		builder.register(12L);
		builder.register(null);

		assertThat(builder.build().getHierarchy()).isDeepEqualTo(new String[][]{
				{"0", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"12", "[10, 13[", "[10, 13[", "[0, 13[", "*"},
				{DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, "*"}
		});
	}
}
