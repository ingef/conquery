package com.bakdata.conquery.util;

import java.io.BufferedReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.bakdata.conquery.apiv1.frontend.VersionContainer;
import com.github.powerlibraries.io.In;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Getter
@Slf4j
public class VersionInfo {

	public final static VersionInfo INSTANCE = new VersionInfo();

	private ZonedDateTime buildTime;
	private String projectVersion;

	/**
	 * Form backend id -> version
	 *
	 * @implNote using {@link TreeMap} to have a stable key order
	 */
	private final Map<String, VersionContainer> formBackendVersions = new TreeMap<>();

	private VersionInfo() {
		try {
			Properties properties = new Properties();
			try (BufferedReader in = In.resource("/git.properties").withUTF8().asReader()) {
				properties.load(in);
			}

			String timeProp = properties.getProperty("build.time");
			try {
				buildTime = ZonedDateTime.parse(timeProp);
			}
			catch (Exception e) {
				log.error("Could not parse date time from git.properties", e);
			}
			projectVersion = properties.getProperty("project.version");
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not read git properties information", e);
		}
	}

	public List<VersionContainer> getVersions() {
		List<VersionContainer> versions = new ArrayList<>();

		versions.add(new VersionContainer("Backend", projectVersion, null));
		versions.addAll(formBackendVersions.values());

		return versions;
	}

	public VersionContainer setFormBackendVersion(VersionContainer version) {
		return formBackendVersions.put(version.name(), version);
	}
}