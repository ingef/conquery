package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import com.bakdata.conquery.models.identifiable.ids.Id;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class TableImportDescriptorId extends NamespacedId<TableImportDescriptor> {

	private final String importDescriptor;

	@Override
	public TableImportDescriptor get(NamespacedStorage namespacedStorage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void collectComponents(List<Object> components) {
		components.add(importDescriptor);
	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
	}

	@Override
	public DatasetId getDataset() {
		throw new UnsupportedOperationException();
	}


	public static enum Parser implements IdUtil.Parser<TableImportDescriptorId> {
		INSTANCE;

		@Override
		public TableImportDescriptorId parseInternally(IdIterator parts) {
			return new TableImportDescriptorId(parts.next());
		}
	}
}
