package org.apache.hadoop.conf;

/**
 * Stub class - see package-info
 */
public class Configuration {

	public Configuration() {

	}

	public Configuration(String _unused) {

	}

	public String get(String configName) {
		return null;
	}

	public Class<?> getClassByName(String className) {
		throw new UnsupportedOperationException();
	}

	public boolean getBoolean(String name, boolean defaultBool) {
		return defaultBool;
	}

	public int getInt(String name, int defaultValue) {
		return defaultValue;
	}
}
