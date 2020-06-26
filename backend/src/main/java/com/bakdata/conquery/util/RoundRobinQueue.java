package com.bakdata.conquery.util;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Queues;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class implementing a queue that is backed by multiple queues at once that are evenly processed, avoiding starvation of jobs when a single producer creates a lot of jobs.
 * @param <E>
 */
@Slf4j
// TODO: 26.06.2020 fk: migrate logging to trace
public class RoundRobinQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

	/**
	 * The backing queues.
	 *
	 * @implNote null denotes no queue, and this queue must not be contiguously filled: Deletions just unset the queue so it is no longer processed.
	 */
    private final Queue<E>[] queues;
    private final Object signal = new Object();

    /**
     * The index to start polling. This is remembered so we don't have a bias towards lower indices.
     */
    private final ThreadLocal<Integer> cycleIndex = ThreadLocal.withInitial(() -> 0);

    public RoundRobinQueue(int capacity) {
        super();
        queues = new Queue[capacity];
    }

	/**
	 * @return Maximum number of queues allowed in this queue.
	 */
	public int getCapacity() {
		return queues.length;
	}

	/**
	 * @return Number of sub-queues.
	 */
	public int getNQueues() {
		return (int) Arrays.stream(queues).filter(Objects::nonNull).count();
	}


	/**
     * Helper class that notifies on {@code signal} when a new object is added to it queue, awakening waiting threads. This effectively implements a semaphore aroud {@code signal} as notify only awakens a single waiting thread.
     */
    @RequiredArgsConstructor
    private static class SignallingForwardingQueue<T> extends ForwardingQueue<T> {

		/**
		 * The original queue.
		 */
		@NonNull
        private final Queue<T> base;

		/**
		 * The signal object to be notified on.
		 */
        private final Object signal;

        @Override
        protected Queue<T> delegate() {
            return base;
        }

		protected void doNotify() {
			log.trace("Awakening a thread for new Work.");
			synchronized (signal) {
				signal.notify();
			}
		}

		/**
		 * Try to offer a new element to the queue, if successful notify on signal, awakening a waiting thread.
		 */
		@Override
        public boolean offer(T element) {
            final boolean offer = super.offer(element);

            if (offer) {
				doNotify();
			}

            return offer;
        }

		/**
		 * Try to add a new element to the queue, if successful notify on signal, awakening a waiting thread.
		 */
        @Override
        public boolean add(T element) {
            final boolean add = super.add(element);

            if (add) {
				doNotify();
			}

            return add;
        }

		/**
		 * Try to offer multiple elements to the queue, if successful notify all threads waiting on signal.
		 *
		 * @implNote this can actually cause some threads to receive nothing, as the collections size might be smaller than the number of waiting threads.
		 */
		@Override
		public boolean addAll(Collection<? extends T> collection) {
			final boolean addAll = super.addAll(collection);

			if(addAll){
				//TODO does this cause problems?
				signal.notifyAll();
			}

			return addAll;
		}
	}

	/**
	 * Create a new queue adding it as sub-queue if there is a slot available.
	 * @throws IllegalStateException when no more slots are available.
	 * @return
	 */
    public Queue<E> createQueue() {
        // TODO: 25.06.2020 FK: add supplier as creation parameter
        final Queue<E> out = new SignallingForwardingQueue<E>(Queues.newConcurrentLinkedQueue(), signal);
		final int free;

        synchronized (queues) {
            free = ArrayUtils.indexOf(queues, null);

            if (free == -1) {
                throw new IllegalStateException(String.format("Queue is full %d", queues.length));
            }

            queues[free] = out;
        }

		log.debug("Create a new Queue at {}. Now have {}", free, getNQueues());

        return out;
    }

	/**
	 * Tries to remove the queue if it is inside.
	 * @param del the Queue to delete.
	 * @return true if the queue was deleted, false if not.
	 */
	public boolean removeQueue(Queue<E> del) {
		final int index;
        synchronized (queues) {
            index = ArrayUtils.indexOf(queues, del);

            if (index == -1) {
                return false;
            }

            queues[index] = null;
        }

		log.debug("Removing Queue at {}. Now have {}", index, getNQueues());

		return true;
    }

	/**
	 * The number of elements over all queues.
	 */
	@Override
    public int size() {
        int sum = 0;
        for (Queue<E> queue : queues) {
            if (queue != null) {
                sum += queue.size();
            }
        }
        return sum;
    }

	/**
	 * @return true, if all queues are empty.
	 */
	@Override
    public boolean isEmpty() {
        for (Queue<E> queue : queues) {
            if (queue != null && !queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

	/**
	 * @param o the Object to check.
	 * @return True, if any queue contains the element.
	 */
	@Override
    public boolean contains(Object o) {
        for (Queue<E> queue : queues) {
            if (queue != null && queue.contains(o)) {
                return true;
            }
        }
        return false;
    }
	/**
	 * @param c the objects to check.
	 * @return True, if all objects are contained in any of the queues.
	 */
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

	/**
	 * Create an iterator looping until the queues are empty.
	 */
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

	/**
	 * Take one element from the queue, blocking until there is a new object available.
	 *
	 * @implNote see {@link RoundRobinQueue::poll()} for implementation details.
	 */
    @NotNull
    @Override
    public E take() throws InterruptedException {
        while (true) {
            E out = poll();
            if (out != null) {
                return out;
            }

			log.trace("Thread[{}], no element in Queue, waiting on Signal.", Thread.currentThread().getName());

			synchronized (signal) {
                signal.wait();
            }

			log.trace("Thread[{}] Awakened for new Work.", Thread.currentThread().getName());
		}
    }


	/**
	 * Try taking one element from the queue, blocking until there is a new object available, or the timeout expires.
	 *
	 * @implNote see {@link RoundRobinQueue::poll()} for implementation details.
	 */
	@Nullable
    @Override
    public E poll(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        E out = poll();

        if (out != null) {
            return out;
        }

		log.trace("Thread[{}], no element in Queue, waiting on Signal.", Thread.currentThread().getName());

		synchronized (signal) {
            unit.timedWait(signal, timeout);
        }

		log.trace("Thread[{}] Awakened for new Work.", Thread.currentThread().getName());

		return poll();
    }


    /**
     * Find an element in any queue. Start searching in a different queue each time.
     */
    @Override
    public E poll() {
    	// The last queue we looked at
        final int begin = cycleIndex.get();

        // Start at the next queue
        for (int offset = 1; offset < queues.length; offset++) {
            final int index = (begin + offset) % (queues.length - 1);
            Queue<E> curr = queues[index];

            if (curr == null) {
                continue;
            }

            // Poll once, if successful update the index of the last polled queue and return the polled element.
            E out = curr.poll();

            if (out != null) {
				log.trace("Thread[{}] found Work in Queue[{}].", Thread.currentThread().getName(), index);
				cycleIndex.set(index);
                return out;
            }
        }

		log.trace("All Queues were empty.");


		// If no queue had elements, return null.
        return null;
    }


	/**
	 * Peek all queues for the next available element.
	 *
	 * @apiNote this method is NOT guaranteed to be stable: peeking multiple times will peek different queues.
	 * @implNote see {@link RoundRobinQueue::poll()} for implementation details.
	 */
	@Override
    public E peek() {
        final int begin = cycleIndex.get();

        for (int offset = 1; offset < queues.length; offset++) {
            final int index = (begin + offset) % (queues.length - 1);
            Queue<E> curr = queues[index];

            if (curr == null) {
                continue;
            }

            E out = curr.peek();

            if (out != null) {
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
