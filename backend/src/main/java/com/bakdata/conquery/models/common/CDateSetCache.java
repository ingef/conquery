package com.bakdata.conquery.models.common;

import java.lang.ref.Cleaner;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.SynchronizedQueue;

@Slf4j
public class CDateSetCache {

	protected final Queue<Container> pool;
	private final Cleaner cleaner = Cleaner.create();

	public CDateSetCache() {
		pool = SynchronizedQueue.synchronizedQueue(new LinkedList<>());
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet back = doAcquire();

		final Container container = new Container(back.getNegativeBits(), back.getPositiveBits());

		cleaner.register(back, () -> {
			log.info("Releasing Object");
			pool.add(container);
		});

		back.clear();

		return back;
	}

	private BitMapCDateSet doAcquire() {
		Container id = pool.poll();

		if (id != null) {
			log.info("Found prior Object");
			return new BitMapCDateSet(id.getLeft(), id.getRight());
		}

		log.info("Creating new new Object.");
		return BitMapCDateSet.create();
	}

	@Data
	@AllArgsConstructor
	private static class Container {
		private final BitSet left;
		private final BitSet right;
	}
}
