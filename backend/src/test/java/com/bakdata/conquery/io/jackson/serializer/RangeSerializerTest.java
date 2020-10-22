package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Random;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@AllArgsConstructor
public class RangeSerializerTest {

	private static final int SEED = 7;

	public static Stream<Range<Integer>> data() {
		Random random = new Random(SEED);
		return  Stream
			.generate(() -> {
				int first = random.nextInt();
				int second = random.nextInt();
	
				if (first < second) {
					return Range.of(first, second);
				}
				return Range.of(second, first);
			})
			.filter(Range::isOrdered)
			.flatMap(range -> Stream.of(
					range,
					Range.exactly(range.getMin()),
					Range.atMost(range.getMin()),
					Range.atLeast(range.getMin())
			))
			.filter(Range::isOrdered)
			.limit(100);
	}

	@ParameterizedTest @MethodSource("data")
	public void test(Range<Integer> range) throws IOException, JSONException {
		SerializationTestUtil
			.forType(new TypeReference<Range<Integer>>() {})
			.test(range);
	}
}
