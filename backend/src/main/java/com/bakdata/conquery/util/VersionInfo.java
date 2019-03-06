package com.bakdata.conquery.util;

import java.io.BufferedReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.github.powerlibraries.io.In;

import lombok.Getter;
import lombok.ToString;

@ToString @Getter
public enum VersionInfo {
	
	INSTANCE;
	
	private final String branch;
	private final ZonedDateTime buildTime;
	private final String buildVersion;
	private final String description;
	private final String projectVersion;
	private final boolean dirty;
	private final String tags;
	
	private VersionInfo() {
		try {
			Properties properties = new Properties();
			try(BufferedReader in = In.resource("/git.properties").withUTF8().asReader()) {
				properties.load(in);
			}
			
			branch =		properties.getProperty("git.branch");
			ZonedDateTime dateTime;
			try {
				dateTime = ZonedDateTime.parse(properties.getProperty("git.build.time"));
			} catch(Exception e) {
				dateTime = null;
				LoggerFactory.getLogger(VersionInfo.class).warn("Could not parse date time from git.properties", e);
			}
			buildTime =		dateTime;
			buildVersion =	properties.getProperty("git.build.version");
			projectVersion =properties.getProperty("project.version");
			description =	properties.getProperty("git.commit.id.describe");
			dirty =			Boolean.parseBoolean(properties.getProperty("git.dirty"));
			tags =			properties.getProperty("git.tags");
			
		} catch (Exception e) {
			throw new IllegalStateException("Could not read git properties information", e);
		}
	}
}