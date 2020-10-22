package com.bakdata.conquery.models.types;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;


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

	@Test
	public void testHexStreamStringType() {
		StringParser parser = new StringParser(new ParserConfig());

		Stream
				.generate(() -> UUID.randomUUID().toString().replace("-", ""))
				.map(String::toUpperCase)
				.mapToInt(v -> {
					try {
						return parser.parse(v);
					} catch (ParsingException e) {
						return 0; // We know that StringTypeVarInt is able to parse our strings.
					}
				})
				.limit(100)
				.forEach(parser::addLine);


		StringTypeEncoded subType = (StringTypeEncoded) parser.findBestType().getType();

		assertThat(subType)
				.isInstanceOf(StringTypeEncoded.class);
		assertThat(subType.getEncoding()).isEqualByComparingTo(StringTypeEncoded.Encoding.Base16UpperCase);
	}
}