package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class TableImportDescriptorId extends AId<TableImportDescriptor> {

	private final String importDescriptor;
	
	@Override
	public void collectComponents(List<Object> components) {
		components.add(importDescriptor);
	}
	
	public static enum Parser implements AId.Parser<TableImportDescriptorId> {
		INSTANCE;

		@Override
		public TableImportDescriptorId parseInternally(IdIterator parts) {
			return new TableImportDescriptorId(parts.next());
		}
	}
}
