package com.bakdata.conquery.models.preproc;

import static com.bakdata.conquery.ConqueryConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import javax.validation.Validator;

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

	public TableImportDescriptor readDescriptor(Validator validator, String tag) throws IOException, JSONException {
		try (Reader in = In.file(descriptionFile).withUTF8().asReader()) {
			TableImportDescriptor descriptor = Jackson.MAPPER.readerFor(TableImportDescriptor.class).readValue(in);

			for (TableInputDescriptor inputDescriptor : descriptor.getInputs()) {
				// TODO: 27.04.2020 Not optimal to be bound to csv.gz
				inputDescriptor.setSourceFile(Preprocessor.getTaggedVersion(csvDirectory.toPath()
																						.resolve(inputDescriptor.getSourceFile().toPath())
																						.toFile(), tag, INPUT_FILE_EXTENSION));
			}

			if (validator != null) {
				ValidatorHelper.failOnError(log, validator.validate(descriptor));
			}
			return descriptor;
		}
	}

	public static InputFile fromDescriptionFile(File descriptionFile, PreprocessingDirectories dirs, String tag) throws IOException {
		descriptionFile = descriptionFile.getAbsoluteFile();
		return fromName(
				dirs,
				descriptionFile.getName().substring(0, descriptionFile.getName().length() - EXTENSION_DESCRIPTION.length()),
				tag
		);
	}

	public static InputFile fromName(PreprocessingDirectories dirs, String extensionlessName, String tag) {
		return builder()
				.descriptionFile(new File(dirs.getDescriptionsDir(), extensionlessName + EXTENSION_DESCRIPTION))
				.preprocessedFile(Preprocessor.getTaggedVersion(new File(dirs.getPreprocessedOutputDir(), extensionlessName + EXTENSION_PREPROCESSED), tag, EXTENSION_PREPROCESSED))
				.csvDirectory(dirs.getCsvDir().getAbsoluteFile())
				.build();
	}
}
