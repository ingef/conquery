package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

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

	public static enum Parser implements IdUtil.Parser<ColumnId> {
		INSTANCE;

		@Override
		public ColumnId parseInternally(IdIterator parts) {
			String column = parts.next();
			TableId parent = TableId.Parser.INSTANCE.parse(parts);
			return new ColumnId(parent, column);
		}
	}
}
