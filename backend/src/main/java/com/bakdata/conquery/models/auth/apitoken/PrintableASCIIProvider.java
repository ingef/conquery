package com.bakdata.conquery.models.auth.apitoken;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PrintableASCIIProvider {
	public final Random random;

	public String getString(int length) {
		return random.ints(0, 128)
				.map(i -> { return (char) i;})
				.filter(this::isValidChar)
				.limit(length)
				.mapToObj(c -> (char) c)
				.map(String::valueOf)
				.collect(Collectors.joining(""));
	}

	private boolean isValidChar(int c) {
		return CharUtils.isAsciiAlphanumeric((char ) c);
	}

}
