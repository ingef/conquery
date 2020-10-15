package com.bakdata.conquery.models.common;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.SynchronizedQueue;

@Slf4j
public class CDateSetCache {

	protected final Queue<Reference<Container>> pool;
	private final Cleaner cleaner = Cleaner.create();

	public CDateSetCache() {
		pool = SynchronizedQueue.synchronizedQueue(new LinkedList<>());
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet back = doAcquire();

		final Container container = new Container(back.getNegativeBits(), back.getPositiveBits());

		cleaner.register(back, () -> {
			log.info("Releasing Object");

			container.getLeft().clear();
			container.getRight().clear();

			pool.add(new WeakReference<>(container));
		});

		return back;
	}

	private BitMapCDateSet doAcquire() {
		Reference<Container> reference;
		Container container;

		while ((reference = pool.poll()) != null && (container = reference.get()) != null) {
			log.info("Found prior Object");
			return new BitMapCDateSet(container.getLeft(), container.getRight());
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
