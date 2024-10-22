package com.bakdata.conquery.io.storage.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.util.NonPersistentStore;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CachedStoreTest {

	private final Store<String, String> backingStore = new NonPersistentStore<>();
	private final CachedStore<String, String> cachedStore = new CachedStore<>(backingStore, CaffeineSpec.parse(""));

	@BeforeEach
	public void beforeEach() {
		// Clear the cache and ensure that the backing store ist also cleared
		cachedStore.clear();
		assertThat(backingStore.getAllKeys().count()).isEqualTo(0);
	}

	@Test
	public void addAndRetrieve() {
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t1_" + i, "%d".formatted(i));
		}

		// Backing Store
		assertThat(backingStore.getAllKeys()).as("All expected keys in backing store")
											 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("t1_%d"::formatted).toArray(String[]::new));


		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Cached Store (getAllKeys only delegates)
		assertThat(cachedStore.getAllKeys()).as("All expected keys in cached store")
											.containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("t1_%d"::formatted).toArray(String[]::new));

		assertThat(cachedStore.getAll()).as("All expected values in cached store")
										.containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));


		// Retrieve individually
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t1_" + i)).isEqualTo("%d".formatted(i));
		}
	}

	@Test
	public void loadAndRetrieve() {
		for (int i : IntStream.range(0, 10).toArray()) {
			backingStore.add("t2_" + i, "%d".formatted(i));
		}

		// Cached Store
		assertThat(cachedStore.getAllKeys()).as("All expected keys in backing store")
											.containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("t2_%d"::formatted).toArray(String[]::new));

		assertThat(cachedStore.getAll()).as("All expected values in backing store")
										.containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));


		// Retrieve individually
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t2_" + i)).isEqualTo("%d".formatted(i));
		}
	}

	@Test
	public void addOverride() {
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t3_" + i, "%d".formatted(i));
		}

		// Retrieve individually to trigger loading/caching
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t3_" + i)).isEqualTo("%d".formatted(i));
		}

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Update values
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t3_" + i, "%d_updated".formatted(i));
		}


		// Retrieve individually
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t3_" + i)).isEqualTo("%d_updated".formatted(i));
		}

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d_updated"::formatted).toArray(String[]::new));
	}

	@Test
	public void update() {
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t4_" + i, "%d".formatted(i));
		}

		// Retrieve individually to trigger loading/caching
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t4_" + i)).isEqualTo("%d".formatted(i));
		}

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Update values
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.update("t4_" + i, "%d_updated".formatted(i));
		}


		// Retrieve individually
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t4_" + i)).isEqualTo("%d_updated".formatted(i));

			assertThat(backingStore.getAll()).as("All expected values in backing store")
											 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d_updated"::formatted).toArray(String[]::new));
		}
	}

	@Test
	public void remove() {
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t5_" + i, "%d".formatted(i));
		}


		assertThat(cachedStore.getAll()).as("All expected values in cached store")
										.containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Remove half of the entries
		for (int i : IntStream.range(0, 5).toArray()) {
			cachedStore.remove("t5_" + i);
		}


		assertThat(cachedStore.getAll()).as("All expected values in cached store")
										 .containsExactlyInAnyOrder(IntStream.range(5, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(5, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Check retrieval of deleted values
		for (int i : IntStream.range(0, 5).toArray()) {
			assertThat(cachedStore.get("t5_" + i)).isNull();
		}
	}
}
