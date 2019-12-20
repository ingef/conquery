package com.bakdata.conquery.models.preproc;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_DESCRIPTION;
import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;

import javax.validation.Validator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.github.powerlibraries.io.In;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
public class InputFile implements Serializable {

	private static final long serialVersionUID = 1L;

	private File csvDirectory;
	private File descriptionFile;
	private File preprocessedFile;

	public ImportDescriptor readDescriptor(Validator validator) throws IOException, JSONException {
		try (Reader in = In.file(descriptionFile).withUTF8().asReader()) {
			ImportDescriptor descriptor = Jackson.MAPPER.readerFor(ImportDescriptor.class).readValue(in);
			for (Input i : descriptor.getInputs()) {
				i.setSourceFile(csvDirectory.toPath().resolve(i.getSourceFile().toPath()).toFile());
			}
			if (validator != null) {
				ValidatorHelper.failOnError(log, validator.validate(descriptor));
			}
			return descriptor;
		}
	}

	public ImportDescriptor readDescriptor() throws IOException, JSONException {
		return this.readDescriptor(null);
	}

	public static InputFile fromDescriptionFile(File descriptionFile, PreprocessingDirectories dirs) throws IOException {
		descriptionFile = descriptionFile.getAbsoluteFile();
		return fromName(
				dirs,
				descriptionFile.getName().substring(0, descriptionFile.getName().length() - EXTENSION_DESCRIPTION.length())
		);
	}

	public static InputFile fromName(PreprocessingDirectories dirs, String extensionlessName) {
		return builder()
				.descriptionFile(new File(dirs.getDescriptions(), extensionlessName + EXTENSION_DESCRIPTION))
				.preprocessedFile(new File(dirs.getPreprocessedOutput(), extensionlessName + EXTENSION_PREPROCESSED))
				.csvDirectory(dirs.getCsv().getAbsoluteFile())
				.build();
	}
}
