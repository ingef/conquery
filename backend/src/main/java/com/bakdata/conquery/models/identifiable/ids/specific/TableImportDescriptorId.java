package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class TableImportDescriptorId extends Id<TableImportDescriptor> {

	private final String importDescriptor;

	@Override
	public void collectComponents(List<Object> components) {
		components.add(importDescriptor);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
	}

	public static enum Parser implements IdUtil.Parser<TableImportDescriptorId> {
		INSTANCE;

		@Override
		public TableImportDescriptorId parseInternally(IdIterator parts) {
			return new TableImportDescriptorId(parts.next());
		}
	}
}
