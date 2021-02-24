package com.bakdata.conquery.models.preproc;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Strings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data
@Slf4j
@RequiredArgsConstructor
@ToString(of = {"descriptionFile", "tag"})
public class PreprocessingJob implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Path csvDirectory;
	private final File descriptionFile;
	private final Path preprocessedOut;

	private final String tag;
	private final TableImportDescriptor descriptor;


	public File getPreprocessedFile() {
		if(Strings.isNullOrEmpty(tag)){
			return preprocessedOut.resolve(descriptor.getName() + EXTENSION_PREPROCESSED).toFile();
		}

		return preprocessedOut.resolve(descriptor.getName() + "." + tag + EXTENSION_PREPROCESSED).toFile();
	}

	public File resolveSourceFile(String fileName) {
		if(Strings.isNullOrEmpty(tag)){
			return csvDirectory.resolve(fileName).toFile();
		}

		String name = fileName;
		final String suffix;

		if(name.endsWith(".csv.gz")){
			name = name.substring(0, name.length() - ".csv.gz".length());
			suffix = ".csv.gz";
		}
		else if(name.endsWith(".csv")){
			name = name.substring(0, name.length() - ".csv".length());
			suffix = ".csv";
		}
		else {
			throw new IllegalArgumentException("Unknown suffix for file " + name);
		}

		return csvDirectory.resolve(name + "." + tag + suffix).toFile();
	}

	/**
	 * Calculate a hash of the descriptor. This is used to only recompute the import when files change.
	 * @param descriptionFile
	 * @param tableImportDescriptor
	 */
	public int calculateValidityHash(File descriptionFile, TableImportDescriptor tableImportDescriptor) throws IOException {
		HashCodeBuilder validityHashBuilder = new HashCodeBuilder();

		validityHashBuilder.append(Files.readString(descriptionFile.toPath(), Charset.defaultCharset()));

		for (TableInputDescriptor input : tableImportDescriptor.getInputs()) {
			validityHashBuilder
					.append(resolveSourceFile(input.getSourceFile()).length());
		}
		return validityHashBuilder.toHashCode();
	}
}
