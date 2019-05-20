package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class ImportColumnId extends AId<ImportColumn> implements NamespacedId {

	private final ImportId imp;
	private final String column;
	
	@Override
	public DatasetId getDataset() {
		return imp.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		imp.collectComponents(components);
		components.add(column);
	}
	
	public static enum Parser implements IId.Parser<ImportColumnId> {
		INSTANCE;
		
		@Override
		public ImportColumnId parseInternally(IdIterator parts) {
			String column = parts.next();
			ImportId parent = ImportId.Parser.INSTANCE.parse(parts);
			return new ImportColumnId(parent, column);
		}
	}
}