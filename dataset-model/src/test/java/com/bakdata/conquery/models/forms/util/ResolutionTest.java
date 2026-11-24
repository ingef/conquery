package com.bakdata.conquery.models.forms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jakarta.validation.ValidationException;

import org.junit.jupiter.api.Test;

class ResolutionTest {

	@Test
	void shouldExposeSupportedAlignmentsAndTheirAmounts() {
		assertResolution(Resolution.COMPLETE, Alignment.NO_ALIGN, 1);
		assertResolution(Resolution.YEARS, Alignment.YEAR, 1);
		assertResolution(Resolution.YEARS, Alignment.QUARTER, 4);
		assertResolution(Resolution.YEARS, Alignment.DAY, 365);
		assertResolution(Resolution.QUARTERS, Alignment.QUARTER, 1);
		assertResolution(Resolution.QUARTERS, Alignment.DAY, 90);
		assertResolution(Resolution.DAYS, Alignment.DAY, 1);

		assertFalse(Resolution.COMPLETE.isAlignmentSupported(Alignment.DAY));
		assertTrue(Resolution.COMPLETE.getAmountForAlignment(Alignment.DAY).isEmpty());
		assertTrue(Alignment.YEAR.getAmountForResolution(Resolution.QUARTERS).isEmpty());
	}

	@Test
	void shouldChooseCompatibleAlignmentOrDefault() {
		assertEquals(Alignment.QUARTER, Resolution.YEARS.getFittingAlignment(Alignment.QUARTER));
		assertEquals(Alignment.QUARTER, Resolution.QUARTERS.getFittingAlignment(Alignment.YEAR));
		assertEquals(Alignment.DAY, Resolution.DAYS.getFittingAlignment(Alignment.QUARTER));
	}

	@Test
	void shouldReturnThisAndCoarserResolutionsInOrder() {
		assertEquals(
				List.of(Resolution.COMPLETE, Resolution.YEARS, Resolution.QUARTERS, Resolution.DAYS),
				Resolution.DAYS.getThisAndCoarserSubdivisions()
		);
	}

	@Test
	void shouldCreateResolutionAndAlignmentMapUsingFallbacks() {
		assertEquals(
				List.of(
						ResolutionAndAlignment.of(Resolution.YEARS, Alignment.YEAR),
						ResolutionAndAlignment.of(Resolution.QUARTERS, Alignment.QUARTER)
				),
				ResolutionAndAlignment.forResolutions(
						List.of(Resolution.YEARS, Resolution.QUARTERS),
						Alignment.YEAR
				)
		);
	}

	@Test
	void shouldRejectUnsupportedResolutionAndAlignmentPair() {
		final ValidationException exception = assertThrows(
				ValidationException.class,
				() -> ResolutionAndAlignment.of(Resolution.QUARTERS, Alignment.YEAR)
		);

		assertEquals(
				"The alignment YEAR is not supported by the resolution QUARTERS",
				exception.getMessage()
		);
	}

	private static void assertResolution(Resolution resolution, Alignment alignment, int amount) {
		assertTrue(resolution.isAlignmentSupported(alignment));
		assertEquals(amount, resolution.getAmountForAlignment(alignment).orElseThrow());
		assertEquals(amount, alignment.getAmountForResolution(resolution).orElseThrow());
	}
}
