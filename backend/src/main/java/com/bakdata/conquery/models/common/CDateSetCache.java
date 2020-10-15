package com.bakdata.conquery.models.common;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Cache to avoid constant reallocation of huge CDateSets, instead cache/reuse their BitSets by way of a {@link Cleaner} that hooks into the GC and will give us those objects back.
 *
 * The underlying data-structures are also maintained as {@link SoftReference} such that they are also subject to GC when memory is demanded.
 */
@Slf4j
public class CDateSetCache {

	protected final Queue<Reference<Container>> pool;
	private final Cleaner cleaner = Cleaner.create();

	public CDateSetCache() {
		pool = new ConcurrentLinkedQueue<>();
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet out = doAcquire();

		// create and maintain hardref on the bitsets
		final Container container = new Container(out.getNegativeBits(), out.getPositiveBits());

		cleaner.register(out, () -> {
			log.trace("Object was released.");

			// Reset the bitsets
			container.getLeft().clear();
			container.getRight().clear();

			// Add reference of container to the bitset, so that they can be freed if memory is demanded.
			pool.add(new SoftReference<>(container));
		});

		return out;
	}

	/**
	 * Try to reuse old BitSets if available. Else create a new one.
	 */
	private BitMapCDateSet doAcquire() {
		Reference<Container> reference;
		Container container;

		log.trace("Have {} Objects available", pool.size());

		while (true) {
			reference = pool.poll();

			// Pool is empty?
			if(reference == null){
				break;
			}

			container = reference.get();

			// Reference was cleared by GC?
			if(container == null){
				continue;
			}

			log.trace("Found prior Object");
			return new BitMapCDateSet(container.getLeft(), container.getRight());
		}

		log.trace("Creating new new Object.");
		return BitMapCDateSet.create();
	}

	@Data
	@AllArgsConstructor
	private static class Container {
		private final BitSet left;
		private final BitSet right;
	}
}
