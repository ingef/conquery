package com.bakdata.conquery.models.query.entity;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.CBlock;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class EntityRow {
	private final Block block;
	private final CBlock cBlock;
	private final Connector connector;
	private final Import imp;
	private final Table table;
}
