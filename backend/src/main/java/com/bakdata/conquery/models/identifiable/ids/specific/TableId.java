package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class TableId extends NamespacedId<Table> {

	private final DatasetId dataset;
	private final String table;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(table);
	}

	@Override
	public void collectIds(Collection<? super Id> into) {
		into.add(this);
		dataset.collectIds(into);
	}

	@Override
	public Table get() {
		return getStorage().getTable(this);
	}

	public static enum Parser implements IdUtil.Parser<TableId> {
		INSTANCE;

		@Override
		public TableId parseInternally(IdIterator parts) {
			String name = parts.next();
			DatasetId dataset = DatasetId.Parser.INSTANCE.parse(parts);
			return new TableId(dataset, name);
		}
	}
}
