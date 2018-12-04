package com.bakdata.conquery.models.config;

import org.hibernate.validator.constraints.Range;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @EqualsAndHashCode
public class ShardConfig {
	@Range(min=10,max=9999)
	private int numberOfShards = 20;
}
