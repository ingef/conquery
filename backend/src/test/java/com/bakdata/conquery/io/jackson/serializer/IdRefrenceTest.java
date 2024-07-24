package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class IdRefrenceTest {

	@Test
	public void testListReferences() throws IOException {
		final ObjectMapper mapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);

		CentralRegistry registry = new CentralRegistry();
		Dataset dataset = new Dataset();
		dataset.setName("dataset");
		Table table = new Table();
		table.setDataset(dataset);
		table.setName("table");
		registry.register(dataset);
		registry.register(table);

		final MetaStorage metaStorage = new MetaStorage(new NonPersistentStoreFactory());

		metaStorage.openStores(null);


		User user = new User("usermail", "userlabel", metaStorage);
		metaStorage.addUser(user);

		String json = mapper.writeValueAsString(
				new ListHolder(
						Collections.singletonList(table),
						Collections.singletonList(user)
				)
		);

		assertThat(json)
				.contains("\"user.usermail\"")
				.contains("\"dataset.table\"");

		new SingletonNamespaceCollection(registry)
				.injectInto(mapper);
		metaStorage.injectInto(mapper);
		ListHolder holder = mapper
				.readerFor(ListHolder.class)
				.readValue(json);

		assertThat(holder.users().get(0)).isSameAs(user);
		assertThat(holder.tables().get(0)).isSameAs(table);
	}

	public record ListHolder(@NsIdRefCollection List<Table> tables, @MetaIdRefCollection List<User> users) {
	}
}
