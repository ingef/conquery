package com.bakdata.conquery.models.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class BucketEntry {
	private final int localEntity;
	private final int event;
}
