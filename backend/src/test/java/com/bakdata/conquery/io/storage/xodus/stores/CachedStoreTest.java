package com.bakdata.conquery.io.storage.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.util.extensions.XodusEnvironmentExtension;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.dropwizard.util.DirectExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CachedStoreTest {

	@RegisterExtension
	private final static XodusEnvironmentExtension ENVIRONMENT = new XodusEnvironmentExtension();
	private final XodusStore xodusStore = new XodusStore(ENVIRONMENT, getClass().getSimpleName(), (env) -> {}, (env) -> {});
	private final SerializingStore<String, String> backingStore = new SerializingStore<>(
			xodusStore,
			null,
			Jackson.MAPPER,
			String.class,
			String.class,
			false,
			false,
			null,
			new DirectExecutorService()

	);
	private final CachedStore<String, String> cachedStore = new CachedStore<>(backingStore, CaffeineSpec.parse("softValues"), null);

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

		cachedStore.loadKeys();

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
	public void add() {
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t3_" + i, "%d".formatted(i));
		}

		// Retrieve individually to trigger loading/caching
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t3_" + i)).isEqualTo("%d".formatted(i));
		}

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));

		// Update values (should not succeed as keys are already present)
		for (int i : IntStream.range(0, 10).toArray()) {
			cachedStore.add("t3_" + i, "%d_updated".formatted(i));
		}


		// Retrieve individually
		for (int i : IntStream.range(0, 10).toArray()) {
			assertThat(cachedStore.get("t3_" + i)).isEqualTo("%d".formatted(i));
		}

		assertThat(backingStore.getAll()).as("All expected values in backing store")
										 .containsExactlyInAnyOrder(IntStream.range(0, 10).mapToObj("%d"::formatted).toArray(String[]::new));
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

		// Check for equality
		final String newVal = "some_new_val";
		cachedStore.update("t4_0", newVal);
		assertThat(cachedStore.get("t4_0")).as("Should reference the same object").isSameAs(newVal);
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
