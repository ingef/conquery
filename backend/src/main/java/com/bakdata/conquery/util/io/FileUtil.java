package com.bakdata.conquery.util.io;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;


@UtilityClass
public class FileUtil {
	public static final Pattern SAVE_FILENAME_REPLACEMENT_MATCHER = Pattern.compile("[^a-zA-Z0-9\\.\\-]");

}
