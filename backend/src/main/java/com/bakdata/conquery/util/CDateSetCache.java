package com.bakdata.conquery.util;

import java.lang.ref.Cleaner;
import java.util.LinkedList;
import java.util.Queue;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CDateSetCache {

	protected final Queue<BitMapCDateSet> pool;
	private final Cleaner cleaner = Cleaner.create();

	public CDateSetCache() {
		pool = new LinkedList<>();
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet back = doAcquire();
		final Proxy out = new Proxy(back);

		cleaner.register(out, () -> {
			back.clear();
			pool.add(back);
		});

		assert back.isEmpty();

		return out;
	}

	private BitMapCDateSet doAcquire() {
		BitMapCDateSet id = pool.poll();

		if (id != null) {
			log.info("Found prior Object");
			return id;
		}

		log.info("Creating new new Object.");
		return BitMapCDateSet.create();
	}

	private static class Proxy extends BitMapCDateSet {

		@Delegate
		private final BitMapCDateSet delegate;

		private Proxy(@NotNull BitMapCDateSet delegate) {
			this.delegate = delegate;
		}
	}
}
