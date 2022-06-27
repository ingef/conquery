package com.bakdata.conquery.io.jackson.serializer;

public enum SerdesTarget {
	MANAGER_AND_SHARD,
	MANAGER,
	SHARD;

	boolean shouldDeserializeField(SerdesTarget fieldTarget) {
		// "this" is the instance/object mapper target
		return fieldTarget == null || this.equals(fieldTarget) || fieldTarget.equals(MANAGER_AND_SHARD);
	}
}
