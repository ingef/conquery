package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StructureNodeId extends Id<StructureNode> {

	private final DatasetId dataset;
	private final StructureNodeId parent;
	private final String structureNode;


	@Override
	public void collectComponents(List<Object> components) {
		if (parent != null) {
			parent.collectComponents(components);
		}
		else {
			dataset.collectComponents(components);
		}
		components.add(structureNode);
	}

	public static enum Parser implements IdUtil.Parser<StructureNodeId> {
		INSTANCE;

		@Override
		public StructureNodeId parseInternally(IdIterator parts) {
			String name = parts.next();
			if (parts.remaining() == 1) {
				DatasetId dataset = DatasetId.Parser.INSTANCE.parse(parts);
				return new StructureNodeId(dataset, null, name);
			}
			StructureNodeId parent = parse(parts);
			return new StructureNodeId(parent.getDataset(), parent, name);
		}
	}
}
