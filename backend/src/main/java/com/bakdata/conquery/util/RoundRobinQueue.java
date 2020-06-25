package com.bakdata.conquery.util;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Queues;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoundRobinQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private static final Queue<?> EMPTY_QUEUE = new ArrayBlockingQueue<>(1);

	private final Queue<E>[] queues;

	private final Object signal = new Object();

	public RoundRobinQueue(int capacity) {
	    queues = new Queue[capacity];
    }

	/**
	 * Clear switch serves as an Atomic comparator to communicate changes of {@code queues} to consuming threads.
	 * @implNote  In order to guarantee atomic updates we use an integer that can be atomically altered, which is not possible with AtomicBooleans
	 * @implSpec Iff clearSwitch != localClearSwitch, cycles is invalid.
	 */
	private final ThreadLocal<Integer> cycleIndex = ThreadLocal.withInitial(() -> 0);

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

        synchronized (queues) {
            final int free = ArrayUtils.indexOf(queues, null);

            if (free == -1) {
                throw new IllegalStateException(String.format("Queue is full %d", this.queues.length));
            }

            queues[free] = out;
        }

        return out;
    }

	public boolean removeQueue(Queue<E> del) {
	    synchronized (queues) {
            final int index = ArrayUtils.indexOf(queues, del);

            if(index == -1) {
                return false;
            }

            queues[index] = null;
        }

        return true;
	}

	@Override
	public int size() {
        int sum = 0;
        for (Queue<E> queue : queues) {
            if(queue != null) {
                sum += queue.size();
            }
        }
        return sum;
	}

	@Override
	public boolean isEmpty() {
        for (Queue<E> queue : queues) {
            if (queue != null && !queue.isEmpty()) {
                return false;
            }
        }
        return true;
	}

    @Override
    public boolean contains(Object o) {
        for (Queue<E> queue : queues) {
            if (queue != null &&  queue.contains(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
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
			E out = poll();
			if(out != null){
				return out;
			}

            synchronized (signal) {
                signal.wait();
            }
        }
	}


	@Nullable
	@Override
	public E poll(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		E out = poll();

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
        final int begin = cycleIndex.get();

        for (int offset = 0; offset < queues.length; offset++) {
            final int index = (begin + offset) % (queues.length - 1);
            Queue<E> curr = queues[index];

            if(curr == null){
                continue;
            }

            E out = curr.poll();

            if(out != null){
                cycleIndex.set(index);
                return out;
            }
        }

        return null;
	}



	@Override
	public E peek() {
		final int begin = cycleIndex.get();

        for (int offset = 0; offset < queues.length; offset++) {
            final int index = (begin + offset) % (queues.length - 1);
            Queue<E> curr = queues[index];

            if(curr == null){
                continue;
            }

            E out = curr.peek();

            if(out != null){
                cycleIndex.set(index);
                return out;
            }
        }

		return null;
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
