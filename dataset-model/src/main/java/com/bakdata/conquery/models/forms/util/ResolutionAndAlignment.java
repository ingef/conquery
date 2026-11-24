package com.bakdata.conquery.models.forms.util;

import java.util.List;
import jakarta.validation.ValidationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Serializable value combining a resolution with a compatible alignment. */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolutionAndAlignment {

	private final Resolution resolution;
	private final Alignment alignment;

	@JsonCreator
	public static ResolutionAndAlignment of(Resolution resolution, Alignment alignment) {
		if (!resolution.isAlignmentSupported(alignment)) {
			throw new ValidationException("The alignment %s is not supported by the resolution %s".formatted(alignment, resolution));
		}
		return new ResolutionAndAlignment(resolution, alignment);
	}

	public static List<ResolutionAndAlignment> forResolutions(List<Resolution> resolutions, Alignment alignmentHint) {
		return resolutions.stream()
				.map(resolution -> of(resolution, resolution.getFittingAlignment(alignmentHint)))
				.toList();
	}

	@Override
	public String toString() {
		return "ResolutionAndAlignment[resolution=%s, alignment=%s]".formatted(resolution, alignment);
	}
}
