package com.bakdata.conquery.models.auth.apitoken;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.CharArrayBuffer;

import java.nio.CharBuffer;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Fills a buffer with random printable ASCII characters that can be used for an {@link ApiToken}.
 * See also {@link ApiTokenCreator}.
 */
@RequiredArgsConstructor
public class PrintableASCIIProvider {
	private final Random random;

	public void fillRemaining(CharArrayBuffer buffer) {
		final int remaining = buffer.capacity() - buffer.length();
		random.ints(0, 128)
				.map(i -> { return (char) i;})
				.filter(this::isValidChar)
				.limit(remaining)
				.mapToObj(c -> (char) c)
				.map(String::valueOf)
				.forEach(buffer::append);
	}

	private boolean isValidChar(int c) {
		return CharUtils.isAsciiAlphanumeric((char ) c);
	}

}
