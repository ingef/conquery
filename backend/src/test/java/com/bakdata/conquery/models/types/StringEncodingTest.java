package com.bakdata.conquery.models.types;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class StringEncodingTest {

	public static final int SEED = 7;

	private UUID randomUUID(Random random) {
		byte[] randomBytes = new byte[16];
		random.nextBytes(randomBytes);

		return UUID.nameUUIDFromBytes(randomBytes);
	}

	@TestFactory
	public Stream<DynamicTest> testEncodings() {

		Random random = new Random(SEED);

		StringTypeEncoded.Encoding encoding = StringTypeEncoded.Encoding.Base64;

		return Stream.generate(() -> randomUUID(random).toString().replace("-", ""))
				.map(uuid -> DynamicTest.dynamicTest(uuid, () -> {
					byte[] decoded = encoding.decode(uuid);
					String encoded = encoding.encode(decoded);

					assertThat(encoded).isEqualTo(uuid);
					assertThat(decoded.length).isLessThan(uuid.length());
				}))
				.limit(100);
	}

	@TestFactory @Execution(ExecutionMode.SAME_THREAD)
	public Stream<DynamicTest> testHexType() throws ParsingException {
		StringType stringType = new StringType();

		StringTypeEncoded type = new StringTypeEncoded(StringTypeEncoded.Encoding.Base16LowerCase, 0, 0);

		Random random = new Random(SEED);

		Map<Integer, String> parsed = Stream.generate(() -> randomUUID(random).toString().replace("-", ""))
											.limit(100)
											.collect(Collectors.toMap(
													v -> {
														try {
															return stringType.parse(v);
														} catch (ParsingException e) {
															return -1;
														}
													}
													, Function.identity()));

		stringType.getDictionary().tryCompress();

		return parsed.keySet().stream()
			.map(parsedId -> DynamicTest.dynamicTest(parsed.get(parsedId), () -> {

			String unparsed = stringType.createScriptValue(
				type.transformFromMajorType(stringType, parsedId)
			);

			assertThat(unparsed)
				.isNotNull()
				.isNotEmpty()
				.isEqualTo(parsed.get(parsedId));
			}))
			.limit(100);
	}

	@Test
	public void testHexStreamStringType() {
		StringType stringType = new StringType();

		Stream
				.generate(() -> UUID.randomUUID().toString().replace("-", ""))
				.map(String::toUpperCase)
				.mapToInt(v -> {
					try {
						return stringType.parse(v);
					} catch (ParsingException e) {
						return 0; // We know that StringType is able to parse our strings.
					}
				})
				.limit(100)
				.forEach(stringType::addLine);


		StringTypeEncoded subType = (StringTypeEncoded) stringType.bestSubType();

		assertThat(subType)
				.isInstanceOf(StringTypeEncoded.class);
		assertThat(subType.getEncoding()).isEqualByComparingTo(StringTypeEncoded.Encoding.Base16UpperCase);
	}
}