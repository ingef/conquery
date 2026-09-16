package com.bakdata.conquery.quarkus.ids;

import java.util.regex.Pattern;

final class IdPart {

	public static final String PART_PATTERN = "[A-Za-z0-9äöüß_/-]+";
	private static final Pattern VALID_PART = Pattern.compile(PART_PATTERN);

	private IdPart() {
	}

	static String requireValid(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " must not be blank.");
		}
		String trimmed = value.trim();
		if (!VALID_PART.matcher(trimmed).matches()) {
			throw new IllegalArgumentException(label + " must match " + PART_PATTERN + " but was: " + value);
		}
		return trimmed;
	}

	static boolean isValid(String value) {
		return value != null && VALID_PART.matcher(value.trim()).matches();
	}

	static String[] split(String value, String label, int minimumParts) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " must not be blank.");
		}
		String[] parts = value.trim().split("\\.", -1);
		if (parts.length < minimumParts) {
			throw new IllegalArgumentException(label + " must have at least " + minimumParts + " part(s): " + value);
		}
		for (int index = 0; index < parts.length; index++) {
			parts[index] = requireValid(parts[index], label + " part " + index);
		}
		return parts;
	}
}
