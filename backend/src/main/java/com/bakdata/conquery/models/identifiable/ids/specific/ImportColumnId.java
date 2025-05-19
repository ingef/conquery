package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
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
public class ImportColumnId extends NamespacedId<ImportColumn>  {

	private final ImportId imp;
	private final String column;

	@Override
	public DatasetId getDataset() {
		return imp.getDataset();
	}

	@Override
	public ImportColumn get() {
		throw new UnsupportedOperationException("%s is never stored".formatted(this.getClass().getSimpleName()));
	}

	@Override
	public void collectComponents(List<Object> components) {
		imp.collectComponents(components);
		components.add(column);
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		imp.collectIds(collect);
	}


	public enum Parser implements IdUtil.Parser<ImportColumnId> {
		INSTANCE;

		@Override
		public ImportColumnId parseInternally(IdIterator parts) {
			String column = parts.next();
			ImportId parent = ImportId.Parser.INSTANCE.parse(parts);
			return new ImportColumnId(parent, column);
		}
	}
}