package com.bakdata.conquery.util;

import java.lang.management.ManagementFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DebugMode {
	@Getter @Setter
	private static boolean active;

	static {
		try {
			active = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
		}
		catch (Exception e) {
			log.info("getting debug Mode failed, it was set to false by default", e);
			active = false;
		}
	}
}
