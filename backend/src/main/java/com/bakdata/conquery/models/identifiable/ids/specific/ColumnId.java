package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ColumnId extends AId<Column> implements NamespacedId {

	private final TableId table;
	private final String column;
	
	@Override
	public DatasetId getDataset() {
		return table.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		table.collectComponents(components);
		components.add(column);
	}
	
	public static enum Parser implements IId.Parser<ColumnId> {
		INSTANCE;
		
		@Override
		public ColumnId parse(PeekingIterator<String> parts) {
			TableId parent = TableId.Parser.INSTANCE.parse(parts);
			return new ColumnId(parent, parts.next());
		}
	}
}
