package com.bakdata.eva.models.translation;

import java.util.Optional;

import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class IdentifiableMocker<T> {

	@Data @AllArgsConstructor
	private static class MockColumn extends Column {

		private ColumnId id;

		public String toString() {
			return id.toString();
		}
	}

	@Data @AllArgsConstructor
	private static class MockTable extends Table {

		private TableId id;

		public String toString() {
			return id.toString();
		}
	}

	@Data
	private static class MockUser extends User {

		private UserId id;

		public MockUser(UserId id) {
			super(null, null);
			this.id = id;
		}

		public String toString() {
			return id.toString();
		}
	}

	@Data @AllArgsConstructor
	private static class MockFilter extends Filter {

		private FilterId id;

		public String toString() {
			return id.toString();
		}

		@Override
		public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
			throw new UnsupportedOperationException(""); //TODO
		}

		@Override
		public Column[] getRequiredColumns() {
			throw new UnsupportedOperationException(""); //TODO
		}

		@Override
		public FilterNode createAggregator(Object filterValue) {
			throw new UnsupportedOperationException(""); //TODO
		}
	}

	@Data @AllArgsConstructor
	private static class MockDataset extends Dataset {

		private DatasetId id;

		public String toString() {
			return id.toString();
		}
	}

	@NoArgsConstructor(access = AccessLevel.PUBLIC)
	public static class MockRegistry extends CentralRegistry {

		@Override
		public <T extends Identifiable<?>> T resolve(IId<T> name) {
			return mockAnswer(name);
		}

		@Override
		public <T extends Identifiable<?>> Optional<T> getOptional(IId<T> name) {
			return Optional.of(resolve(name));
		}
	}



	public static <X extends Identifiable> X mockAnswer(IId id) {
		if (id instanceof DatasetId) {
			return (X) new MockDataset((DatasetId) id);
		}
		else if (id instanceof ColumnId) {
			return (X) new MockColumn((ColumnId) id);
		}
		else if (id instanceof TableId) {
			return (X) new MockTable((TableId) id);
		}
		else if (id instanceof FilterId) {
			return (X) new MockFilter((FilterId) id);
		}
		else if (id instanceof UserId) {
			return (X) new MockUser((UserId) id);
		}
		else
			throw new IllegalStateException("Unknown id type " + id.getClass());
	}
}
