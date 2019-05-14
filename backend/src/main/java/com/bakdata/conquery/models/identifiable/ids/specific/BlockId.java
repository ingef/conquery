package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class BlockId extends AId<Block> implements NamespacedId {

	private final ImportId imp;
	private final int entity;
	
	@Override
	public DatasetId getDataset() {
		return imp.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		imp.collectComponents(components);
		components.add(entity);
	}
	
	public static enum Parser implements IId.Parser<BlockId> {
		INSTANCE;
		
		@Override
		public BlockId parseInternally(IdIterator parts) {
			int entity = Integer.parseInt(parts.next());
			ImportId parent = ImportId.Parser.INSTANCE.parse(parts);
			return new BlockId(parent, entity);
		}
	}
}