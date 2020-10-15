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
 * <p>
 * The underlying data-structures are also maintained as {@link SoftReference} such that they are also subject to GC when memory is demanded.
 */
@Slf4j
public class CDateSetCache {

	private static final CDateSetCache dateSetCache = new CDateSetCache();
	protected final Queue<Reference<Container>> pool;
	private final Cleaner cleaner = Cleaner.create();

	private CDateSetCache() {
		pool = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Preallocate the DateSet, such that typical queries don't have to grow them while executing.
	 * The numbers are just best guesses and can be fine tuned if desired but configuration is probably not important.
	 */
	public static BitMapCDateSet createPreAllocatedDateSet() {
		return dateSetCache.acquire();
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet out = doAcquire();

		// create and maintain hardref on the bitsets
		final Container container = new Container(out.getNegativeBits(), out.getPositiveBits());

		cleaner.register(out, () -> {
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

		while (true) {
			reference = pool.poll();

			// Pool is empty?
			if (reference == null) {
				break;
			}

			container = reference.get();

			// Reference was cleared by GC?
			if (container == null) {
				continue;
			}

			return new BitMapCDateSet(container.getLeft(), container.getRight());
		}

		return BitMapCDateSet.create();
	}

	@Data
	@AllArgsConstructor
	private static class Container {
		private final BitSet left;
		private final BitSet right;
	}
}
