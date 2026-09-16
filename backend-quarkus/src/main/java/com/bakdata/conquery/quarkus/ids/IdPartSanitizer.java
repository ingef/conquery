package com.bakdata.conquery.quarkus.ids;

public final class IdPartSanitizer {

	private IdPartSanitizer() {
	}

	public static String sanitize(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " must not be blank.");
		}
		String sanitized = value.trim()
								.replace('.', '_')
								.replaceAll("[^A-Za-z0-9äöü_/-]+", "_")
								.replaceAll("_+", "_")
								.replaceAll("^_+|_+$", "");
		if (sanitized.isBlank() || !IdPart.isValid(sanitized)) {
			throw new IllegalArgumentException(label + " cannot be sanitized into a valid id part: " + value);
		}
		return sanitized;
	}
}
