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
import com.bakdata.conquery.util.extensions.MetaStorageExtension;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class IdRefrenceTest {

	@RegisterExtension
	private final static MetaStorageExtension META_STORAGE_EXTENSION = new MetaStorageExtension();

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

		MetaStorage metaStorage = META_STORAGE_EXTENSION.getMetaStorage();

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

		assertThat(holder.getUsers().get(0)).isSameAs(user);
		assertThat(holder.getTables().get(0)).isSameAs(table);
	}

	/**
	 * @implNote this needs to be a class, because jackson ignores NsIdRefCollection on records
	 */
	@Getter
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class ListHolder {
		@NsIdRefCollection
		private final List<Table> tables;
		@MetaIdRefCollection
		private final List<User> users;
	}
}
