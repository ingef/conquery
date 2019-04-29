package com.bakdata.conquery.util;

import java.io.BufferedReader;
import java.time.ZonedDateTime;
import java.util.Properties;

import com.github.powerlibraries.io.In;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString @Getter @Slf4j
public class VersionInfo {
	
	public final static VersionInfo INSTANCE = new VersionInfo();
	
	private ZonedDateTime buildTime;
	private String projectVersion;
	
	private VersionInfo() {
		try {
			Properties properties = new Properties();
			try(BufferedReader in = In.resource("/git.properties").withUTF8().asReader()) {
				properties.load(in);
			}
			
			String timeProp = properties.getProperty("build.time");
			try {
				buildTime = ZonedDateTime.parse(timeProp);
			}
			catch(Exception e) {
				log.error("Could not parse date time from git.properties", e);
			}
			projectVersion =properties.getProperty("project.version");
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not read git properties information", e);
		}
	}
}