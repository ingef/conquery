package com.bakdata.conquery.models.events;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CBlock extends IdentifiableImpl<CBlockId> {
	
	@Valid
	private BlockId block;
	@NotNull @Valid
	private ConnectorId connector;
	@Valid
	private List<int[]> mostSpecificChildren;
	
	public CBlock(BlockId block, ConnectorId connector) {
		this.block = block;
		this.connector = connector;
	}
	
	@Override @JsonIgnore
	public CBlockId createId() {
		return new CBlockId(block, connector);
	}
}
