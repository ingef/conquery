package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CBlockId extends Id<CBlock> implements NamespacedId {

	private final BucketId bucket;
	private final ConnectorId connector;

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public void collectComponents(List<Object> components) {
		bucket.collectComponents(components);
		connector.collectComponents(components);
	}

	public static enum Parser implements IdUtil.Parser<CBlockId> {
		INSTANCE;

		@Override
		public CBlockId parseInternally(IdIterator parts) {
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts.splitOff(3));
			BucketId block = BucketId.Parser.INSTANCE.parse(parts);
			return new CBlockId(block, connector);
		}
	}
}