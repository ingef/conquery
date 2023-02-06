package com.bakdata.conquery.util;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import javax.validation.constraints.NotNull;

import com.google.common.util.concurrent.Uninterruptibles;
import lombok.Builder;

@Builder
public class Wait {

	@NotNull
	private final Duration stepTime;

	@NotNull
	private final Duration total;

	public void until(BooleanSupplier condition) {
		final long attempts = total.dividedBy(stepTime);

		long attempt = 0;

		while (!condition.getAsBoolean()) {
			if (attempt >= attempts) {
				throw new RuntimeException("Failed while waiting after " + attempt + " attempts");
			}
			Uninterruptibles.sleepUninterruptibly(stepTime);
			attempt++;
		}
	}
}
