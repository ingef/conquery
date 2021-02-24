package com.bakdata.conquery.models.preproc;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

import com.google.common.base.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@RequiredArgsConstructor
@ToString(of = {"descriptionFile", "tag"})
public class PreprocessingJob implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Path csvDirectory;
	private final File descriptionFile;
	private final Path preprocessedOut;

	private final Optional<String> tag;
	private final TableImportDescriptor descriptor;


	public File getPreprocessedFile() {
		if(!tag.isPresent()){
			return preprocessedOut.resolve(descriptor.getName() + EXTENSION_PREPROCESSED).toFile();
		}

		return preprocessedOut.resolve(descriptor.getName() + "." + tag.get() + EXTENSION_PREPROCESSED).toFile();
	}

}
