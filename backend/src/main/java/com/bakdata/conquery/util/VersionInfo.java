package com.bakdata.conquery.util;

import java.io.BufferedReader;
import java.time.ZonedDateTime;
import java.util.Properties;

import com.github.powerlibraries.io.In;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Getter
@Slf4j
public class VersionInfo {

	public final static VersionInfo INSTANCE = new VersionInfo();

	private String branch;
	private ZonedDateTime buildTime;
	private String buildVersion;
	private String description;
	private String projectVersion;
	private boolean dirty;
	private String tags;

	private VersionInfo() {
		try {
			Properties properties = new Properties();
			try (BufferedReader in = In.resource("/git.properties").withUTF8().asReader()) {
				properties.load(in);
			}

			branch = properties.getProperty("git.branch");
			String timeProp = properties.getProperty("git.build.time");
			try {
				buildTime = ZonedDateTime.parse(timeProp);
			}
			catch (Exception e) {
				log.error("Could not parse date time from git.properties", e);
			}
			buildVersion = properties.getProperty("git.build.version");
			projectVersion = properties.getProperty("project.version");
			description = properties.getProperty("git.commit.id.describe");
			dirty = Boolean.parseBoolean(properties.getProperty("git.dirty"));
			tags = properties.getProperty("git.tags");

		}
		catch (Exception e) {
			throw new IllegalStateException("Could not read git properties information", e);
		}
	}
}