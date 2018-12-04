package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class StructureNodeId extends AId<StructureNode> {

	private final StructureNodeId parent;
	private final String structureNode;
	
	
	@Override
	public void collectComponents(List<Object> components) {
		parent.collectComponents(components);
		components.add(structureNode);
	}
	
	public static enum Parser implements IId.Parser<StructureNodeId> {
		INSTANCE;
		
		@Override
		public StructureNodeId parse(Iterator<String> parts) {
			StructureNodeId parent = StructureNodeId.Parser.INSTANCE.parse(parts);
			StructureNodeId result = new StructureNodeId(parent, parts.next());
			while(parts.hasNext()) {
				result = new StructureNodeId(result, parts.next());
			}
			return result;
		}
	}
}
