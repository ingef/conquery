package com.bakdata.conquery.util;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.common.util.concurrent.Uninterruptibles;

import lombok.Builder;

@Builder
public class Wait {

	private final int stepTime;
	@Builder.Default
	private final TimeUnit stepUnit = TimeUnit.MILLISECONDS;
	@Builder.Default
	private final int attempts = -1;
	
	
	public void until(BooleanSupplier condition) {
		int attempt = 0;
		while(!condition.getAsBoolean()) {
			if(attempts != -1 && attempt >= attempts) {
				throw new RuntimeException("Failed while waiting after "+attempt+" attempts");
			}
			Uninterruptibles.sleepUninterruptibly(stepTime, stepUnit);
			attempt++;
		}
	}
}
