package com.bakdata.conquery.util;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class RoundRobinQueueTest {

	@Test
	public void test() {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>();

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);



		assertThat(queue.contains(1)).isTrue();
		assertThat(queue.contains(2)).isTrue();
		assertThat(queue.contains(3)).isFalse();

		final Iterator<Integer> iterator = queue.iterator();

		assertThat(iterator.next()).isEqualTo(1);
		assertThat(first).isEmpty();
		assertThat(iterator.next()).isEqualTo(2);
		assertThat(second).isEmpty();
		assertThat(iterator.next()).isEqualTo(null);
		assertThat(queue).isEmpty();
	}

	@Test
	public void testIterator() {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>();

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);

		final List<Integer> out = new ArrayList<>();

		for (Integer integer : queue) {
			out.add(integer);
		}

		assertThat(out).containsExactly(1, 2);
	}

	@Test
	public void testNewQueue() {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>();

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);


		final Iterator<Integer> iterator = queue.iterator();


		assertThat(iterator.next()).isEqualTo(1);
		assertThat(iterator.next()).isEqualTo(2);
		assertThat(iterator.next()).isEqualTo(null);

		final Queue<Integer> third = queue.createQueue();
		third.add(3);


		assertThat(iterator.next()).isEqualTo(3);
	}


	@Test
	public void parPutSynTake() throws InterruptedException {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>();

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		first.add(1);
		second.add(2);

		new Thread(() -> {
			try {
				TimeUnit.SECONDS.sleep(5);
				first.add(3);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();


		assertThat(queue.take()).isEqualTo(1);
		assertThat(queue.take()).isEqualTo(2);
		assertThat(queue.take()).isEqualTo(3);
	}

	@Test
	public void parPutParTake() throws InterruptedException {
		final RoundRobinQueue<Integer> queue = new RoundRobinQueue<>();

		final Queue<Integer> first = queue.createQueue();
		final Queue<Integer> second = queue.createQueue();

		IntStream.range(0,5).forEach(first::add);
		IntStream.range(5,10).forEach(second::add);

		final Set<Integer> found = Collections.synchronizedSet(new HashSet<>());

		final List<Thread> threads = new ArrayList<>();

		final Thread thread = new Thread(() -> {
			try {
				for (int value = 10; value < 20; value++) {
					TimeUnit.SECONDS.sleep(1);
					queue.createQueue().offer(value);
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		threads.add(thread);

		thread.start();

		for (int value = 0; value < 10; value++) {
			final Thread thread1 = new Thread(() -> {
				try {
					final Integer taken = queue.take();
					assertThat(found.add(taken))
							.describedAs("Value=%d", taken)
							.isTrue();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			});

			thread1.start();

			threads.add(thread1);
		}

		assertThat(queue).isEmpty();

		for (Thread thread1 : threads) {
			thread1.join();
		}
	}



}