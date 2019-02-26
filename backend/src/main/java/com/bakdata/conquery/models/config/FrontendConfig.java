package com.bakdata.conquery.models.config;

import com.bakdata.conquery.util.VersionInfo;

import groovy.transform.ToString;
import lombok.Getter;
import lombok.Setter;

@ToString
public class FrontendConfig {
	
	@Getter @Setter
	private String version = VersionInfo.INSTANCE.getDescription();
	@Getter @Setter
	private boolean production = true;
}