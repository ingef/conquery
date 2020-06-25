package com.bakdata.conquery.util;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoundRobinQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

	private final Collection<Queue<E>> queues = new ArrayList<>();

	private final Object signal = new Object();

	/**
	 * Clear switch serves as an Atomic comparator to communicate changes of {@code queues} to consuming threads.
	 * @implNote  In order to guarantee atomic updates we use an integer that can be atomically altered, which is not possible with AtomicBooleans
	 * @implSpec Iff clearSwitch != localClearSwitch, cycles is invalid.
	 */
	private final AtomicInteger clearSwitch = new AtomicInteger(1);
	private final ThreadLocal<Integer> localClearSwitch = ThreadLocal.withInitial(() -> 0);

	private final ThreadLocal<Iterator<Queue<E>>> cycles = new ThreadLocal<>();

	/**
	 * Helper class that notifies on {@code signal} when a new object is added to any queue, awakening all waiting threads.
	 */
	@RequiredArgsConstructor
	private static class SignallingForwardingQueue<T> extends ForwardingQueue<T> {

		@NonNull
		private final Queue<T> base;
		private final Object signal;

		@Override
		protected Queue<T> delegate() {
			return base;
		}

		@Override
		public boolean offer(T element) {
			final boolean offer = super.offer(element);

			if(offer){
				synchronized (signal) {
					signal.notify();
				}
			}

			return offer;
		}

		@Override
		public boolean add(T element) {
			final boolean add = super.add(element);

			if(add){
				synchronized (signal) {
					signal.notify();
				}
			}

			return add;
		}
	}

	public Queue<E> createQueue() {
		// TODO: 25.06.2020 FK: add supplier as creation parameter
		final Queue<E> out = new SignallingForwardingQueue<>(Queues.newConcurrentLinkedQueue(), signal);
		queues.add(out);
		clearSwitch.incrementAndGet();

		return out;
	}

	public boolean removeQueue(Queue<E> del) {
		final boolean remove = queues.remove(del);
		clearSwitch.incrementAndGet();

		return remove;
	}

	@Override
	public int size() {
		return queues.stream().mapToInt(Queue::size).sum();
	}

	@Override
	public boolean isEmpty() {
		return queues.stream().allMatch(Queue::isEmpty);
	}

	@Override
	public boolean contains(Object o) {
		return queues.stream().anyMatch(q -> q.contains(o));
	}

	@NotNull
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return !isEmpty();
			}

			@Override
			public E next() {
				return poll();
			}
		};
	}


	@NotNull
	@Override
	public E take() throws InterruptedException {
		while(true){
			final E out = poll();
			if(out != null){
				return out;
			}

			doWait();
		}
	}

	protected void doWait() throws InterruptedException {
		synchronized (signal) {
			signal.wait();
		}
	}

	@Nullable
	@Override
	public E poll(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		final E out = poll();

		if(out != null){
			return out;
		}

		synchronized (signal) {
			unit.timedWait(signal, timeout);
		}

		return poll();
	}



	/**
	 * Do one cycle, starting from the next Queue polling all sub-queues.
	 */
	@Override
	public E poll() {
		final Queue<E> begin = nextQueue();
		Queue<E> curr = begin;

		do {
			E out = curr.poll();

			if(out != null){
				return out;
			}

			curr = nextQueue();

		} while (curr != begin);

		return null;
	}



	@Override
	public E peek() {
		final Queue<E> begin = nextQueue();
		Queue<E> curr = begin;

		do {
			E out = curr.peek();

			if(out != null){
				return out;
			}

			curr = nextQueue();

		} while (curr != begin);

		return null;
	}


	/**
	 * Advance the ThreadLocal Cycle of Queues by one.
	 * Reset
	 */
	private Queue<E> nextQueue() {
		final int global = clearSwitch.getOpaque();
		if(global != localClearSwitch.get()){
			localClearSwitch.set(global);
			cycles.set(Iterators.cycle(queues));
		}

		return cycles.get().next();
	}


	//=====================================
	//=       Unsupported Methods         =
	//=====================================

	@Override
	public int drainTo(@NotNull Collection<? super E> c) {
		// TODO: 25.06.2020 implement this?
		return 0;
	}

	@Override
	public int drainTo(@NotNull Collection<? super E> c, int maxElements) {
		// TODO: 25.06.2020 implement this?
		return 0;
	}

	@NotNull
	@Override
	public Object[] toArray() {
		// TODO: 25.06.2020 implement this?
		return new Object[0];
	}

	@NotNull
	@Override
	public <T1> T1[] toArray(@NotNull T1[] a) {
		// TODO: 25.06.2020 implement this?
		return null;
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return c.stream().allMatch(this::contains);
	}

	@Override
	public boolean remove(Object o) {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}


	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public boolean offer(E t) {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public void put(@NotNull E e) throws InterruptedException {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public boolean offer(E e, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public E remove() {
		throw new IllegalStateException("Cannot directly mutate RoundRobin Queues");
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}
}
