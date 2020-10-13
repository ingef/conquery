package com.bakdata.conquery.models.events;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter
@Data @ToString
public class BucketEntry {
	private final int localEntity;
	private final int event;
}
