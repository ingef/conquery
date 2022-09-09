package com.bakdata.conquery.models.forms.arx;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AttributeTypeBuilderTest {

	@Test
	public void integerIntervalBuilder() {
		final AttributeTypeBuilder.IntegerInterval builder = new AttributeTypeBuilder.IntegerInterval();

		builder.register(Long.valueOf(0));
		builder.register(Long.valueOf(12));

		assertThat(builder.build().getHierarchy()).isDeepEqualTo(new String[][]{
				{"0", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"1", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"2", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"3", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"4", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				{"5", "[5, 10[", "[0, 10[", "[0, 13[", "*"},
				{"6", "[5, 10[", "[0, 10[", "[0, 13[", "*"},
				{"7", "[5, 10[", "[0, 10[", "[0, 13[", "*"},
				{"8", "[5, 10[", "[0, 10[", "[0, 13[", "*"},
				{"9", "[5, 10[", "[0, 10[", "[0, 13[", "*"},
				{"10", "[10, 13[", "[10, 13[", "[0, 13[", "*"}
		});
	}
}
