package com.bakdata.conquery.models.query.entity;

import com.bakdata.conquery.models.events.CBlock;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class EntityRow {
	//TODO Only the CBlock is used, remove this class.
	private final CBlock cBlock;
}
