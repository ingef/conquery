package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class CBlockId extends AId<CBlock> implements NamespacedId {

	private final BlockId block;
	private final ConnectorId connector;
	
	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		block.collectComponents(components);
		connector.collectComponents(components);
	}
	
	public static enum Parser implements IId.Parser<CBlockId> {
		INSTANCE;
		
		@Override
		public CBlockId parse(PeekingIterator<String> parts) {
			BlockId block = BlockId.Parser.INSTANCE.parse(parts);
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts);
			return new CBlockId(block, connector);
		}
	}
}