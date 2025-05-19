package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
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
public class ImportId extends NamespacedId<Import>  {

	private final TableId table;
	private final String tag;

	@Override
	public DatasetId getDataset() {
		return table.getDataset();
	}

	@Override
	public Import get() {
		return getDomain().getStorage(getDataset()).getImport(this);
	}

	@Override
	public void collectComponents(List<Object> components) {
		table.collectComponents(components);
		components.add(tag);
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		table.collectIds(collect);
	}


	public enum Parser implements IdUtil.Parser<ImportId> {
		INSTANCE;

		@Override
		public ImportId parseInternally(IdIterator parts) {
			String tag = parts.next();
			TableId parent = TableId.Parser.INSTANCE.parse(parts);
			return new ImportId(parent, tag);
		}
	}
}