package com.bakdata.conquery.models.forms.arx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.deidentifier.arx.DataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AttributeTypeBuilderTest {

	@ParameterizedTest
	@CsvSource({
			"1,1",
			"2,2",
			"3,3",
			"4,3",
			"5,4",
			"8,4",
			"16,5",
			"200,9"
	})
	public void calculateLevels(int inputNodes, int expectedLevels) {
		assertThat(AttributeTypeBuilder.countLevelsFromLeafNodes(inputNodes)).isEqualTo(expectedLevels);
	}

	@Test
	public void integerIntervalBuilderDefaultBucketSize() {
		final AttributeTypeBuilder.IntegerInterval builder = new AttributeTypeBuilder.IntegerInterval();

		builder.register(0L);
		builder.register(12L);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		assertThat(Arrays.asList(actual)).containsExactlyInAnyOrder(
				new String[]{"0", "[0, 5[", "[0, 10[", "[0, 13[", "*"},
				new String[]{"12", "[10, 13[", "[10, 13[", "[0, 13[", "*"},
				new String[]{DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, "*"}
		);
	}


	@Test
	public void integerIntervalBuilderOneBucket() {
		final AttributeTypeBuilder.IntegerInterval builder = new AttributeTypeBuilder.IntegerInterval();

		builder.register(0);
		builder.register(4);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		assertThat(Arrays.asList(actual)).containsExactlyInAnyOrder(
				new String[]{"0", "[0, 5[", "*"},
				new String[]{"4", "[0, 5[", "*"},
				new String[]{DataType.NULL_VALUE, DataType.NULL_VALUE, "*"}
		);
	}

	@Test
	public void integerIntervalBuilderMaximumBucketCount() {
		final AttributeTypeBuilder.IntegerInterval builder = new AttributeTypeBuilder.IntegerInterval();

		builder.register(0);
		builder.register(100);
		builder.register(AttributeTypeBuilder.MAX_BUCKETS_LOWEST_LEVEL * AttributeTypeBuilder.BUCKET_SIZE + 1);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		// First dimension: registered values
		// Second dimension: actual-value-level (1), bucketing-levels (11), NULL-merge-level (1) = 13
		assertThat(actual).hasDimensions(4, 13);
	}

	@Test
	public void decimalIntervalBuilderOneBucket() {
		final AttributeTypeBuilder.DecimalInterval builder = new AttributeTypeBuilder.DecimalInterval();

		builder.register(0.0);
		builder.register(4.0);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		assertThat(Arrays.asList(actual)).containsExactlyInAnyOrder(
				new String[]{"0.0", "[0.0, 4.001[", "*"},
				new String[]{"4.0", "[0.0, 4.001[", "*"},
				new String[]{DataType.NULL_VALUE, DataType.NULL_VALUE, "*"}
		);
	}

	@Test
	public void decimalIntervalBuilderDefaultBucketSize() {
		final AttributeTypeBuilder.DecimalInterval builder = new AttributeTypeBuilder.DecimalInterval();

		builder.register(0.0);
		builder.register(12.0);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		assertThat(Arrays.asList(actual)).containsExactlyInAnyOrder(
				new String[]{"0.0", "[0.0, 5.0[", "[0.0, 10.0[", "[0.0, 12.001[", "*"},
				new String[]{"12.0", "[10.0, 12.001[", "[10.0, 12.001[", "[0.0, 12.001[", "*"},
				new String[]{DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, DataType.NULL_VALUE, "*"}
		);
	}

	@Test
	public void decimalIntervalBuilderMaximumBucketCount() {
		final AttributeTypeBuilder.DecimalInterval builder = new AttributeTypeBuilder.DecimalInterval();

		builder.register(0.0);
		builder.register(100.0);
		builder.register(AttributeTypeBuilder.MAX_BUCKETS_LOWEST_LEVEL * AttributeTypeBuilder.BUCKET_SIZE + 1);
		builder.register(null);

		final String[][] actual = builder.build().getHierarchy();
		// First dimension: registered values
		// Second dimension: actual-value-level (1), bucketing-levels (11), NULL-merge-level (1) = 13
		assertThat(actual).hasDimensions(4, 13);
	}

}
