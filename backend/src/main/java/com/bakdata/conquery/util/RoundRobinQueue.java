package com.bakdata.conquery.util;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoundRobinQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

	private final Collection<Queue<E>> queues;
	private final Map<Thread, Iterator<Queue<E>>> cycles;

	private final Object signal = new Object();

	public RoundRobinQueue(){
		this.queues = new ArrayList<>();
		cycles = Maps.newConcurrentMap();
	}

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
		final Queue<E> out = new SignallingForwardingQueue<E>(Queues.newConcurrentLinkedQueue(), signal);
		queues.add(out);
		cycles.clear();

		return out;
	}

	public boolean removeQueue(Queue<E> del) {
		final boolean remove = queues.remove(del);
		cycles.clear();

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
	 */
	private Queue<E> nextQueue() {
		// TODO: 25.06.2020 this map is still a global lock, consider an atomic switch with a ThreadLocal comparator, instead of clearing the map.
		return cycles.computeIfAbsent(Thread.currentThread(), ignored -> Iterators.cycle(queues)).next();
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
