package com.bakdata.conquery.util;

import java.lang.ref.Cleaner;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.SynchronizedQueue;

@Slf4j
public class CDateSetCache {

	protected final Queue<BitMapCDateSet> pool;
	private final Cleaner cleaner = Cleaner.create();

	public CDateSetCache() {
		pool = SynchronizedQueue.synchronizedQueue(new LinkedList<>());
	}

	public BitMapCDateSet acquire() {
		final BitMapCDateSet back = doAcquire();
		final Proxy proxy = new Proxy(back);

		cleaner.register(proxy, () -> {
			log.info("Releasing Object");
			back.clear();
			pool.add(back);
		});

		back.clear();

		return proxy;
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

		private final BitMapCDateSet delegate;

		private Proxy(@NotNull BitMapCDateSet delegate) {
			this.delegate = delegate;
		}

		public boolean contains(LocalDate value) {
			return delegate.contains(value);
		}

		public boolean contains(int value) {
			return this.delegate.contains(value);
		}

		public void clear() {
			this.delegate.clear();
		}

		public void addAll(BitMapCDateSet other) {
			if (other instanceof Proxy) {
				delegate.addAll(((Proxy) other).delegate);
			}
			else {
				this.delegate.addAll(other);
			}
		}

		public void removeAll(BitMapCDateSet other) {
			if (other instanceof Proxy) {
				delegate.removeAll(((Proxy) other).delegate);
			}
			else {
				this.delegate.removeAll(other);
			}
		}

		public void addAll(Iterable<CDateRange> ranges) {
			this.delegate.addAll(ranges);
		}

		public boolean intersects(CDateRange range) {
			return this.delegate.intersects(range);
		}

		public CDateRange span() {
			return this.delegate.span();
		}

		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		public boolean isAll() {
			return this.delegate.isAll();
		}

		public int getMaxValue() {
			return this.delegate.getMaxValue();
		}

		public int getMinValue() {
			return this.delegate.getMinValue();
		}

		public void add(CDateRange rangeToAdd) {
			this.delegate.add(rangeToAdd);
		}

		public void remove(CDateRange rangeToAdd) {
			this.delegate.remove(rangeToAdd);
		}

		public void retainAll(BitMapCDateSet retained) {
			if (retained instanceof Proxy) {
				delegate.retainAll(((Proxy) retained).delegate);
			}
			else {
				this.delegate.retainAll(retained);
			}
		}

		public void retainAll(CDateRange retained) {
			this.delegate.retainAll(retained);
		}

		public void maskedAdd(@NonNull CDateRange toAdd, BitMapCDateSet mask) {
			if (mask instanceof Proxy) {
				delegate.maskedAdd(toAdd, ((Proxy) mask).delegate);
			}
			else {
				this.delegate.maskedAdd(toAdd, mask);
			}
		}

		public Long countDays() {
			return this.delegate.countDays();
		}

		public Collection<CDateRange> asRanges() {
			return this.delegate.asRanges();
		}

		@Override
		public String toString() {
			return delegate.toString();
		}
	}
}
