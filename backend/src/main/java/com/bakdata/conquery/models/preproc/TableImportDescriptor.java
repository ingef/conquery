package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Combines potentially multiple input files to be loaded into a single table. Describing their respective transformation. All Inputs must produce the same types of outputs.
 * <p>
 * For further detail see {@link TableInputDescriptor}, and {@link Preprocessor}.
 * <p>
 * This file describes an `import.json` used as description for the `preprocess` command.
 */
@Getter
@Setter
@Slf4j
@JsonIgnoreProperties({"label"})
@ToString(onlyExplicitlyIncluded = true)
public class TableImportDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotEmpty
	@ToString.Include
	private String name;

	/**
	 * Target table to load the import to.
	 */
	@NotEmpty
	@ToString.Include
	private String table;

	/**
	 * A single source input.
	 */
	@NotEmpty
	@Valid
	private TableInputDescriptor[] inputs;

	public static TableImportDescriptor read(File descriptionFile) throws IOException {
		return Jackson.MAPPER.readerFor(TableImportDescriptor.class).readValue(descriptionFile);
	}

	@JsonIgnore
	@ValidationMethod(message = "The output of each input needs the same number of output columns of the same type and name")
	public boolean isSameTypesInEachInput() {
		if (inputs.length == 1) {
			return true;
		}
		List<MajorTypeId[]> types = new ArrayList<>();

		for (TableInputDescriptor input : inputs) {
			MajorTypeId[] inp = Arrays.stream(input.getOutput()).map(OutputDescription::getResultType).toArray(MajorTypeId[]::new);

			for (MajorTypeId[] out : types) {
				if (!Arrays.equals(inp, out)) {
					return false;
				}
			}
			types.add(inp);
		}
		return true;
	}

	/**
	 * Calculate a hash of the descriptor. This is used to only recompute the import when files change.
	 */
	public int calculateValidityHash(Path csvDirectory, Optional<String> tag) {
		HashCodeBuilder validityHashBuilder = new HashCodeBuilder();

		validityHashBuilder.append(getName()).append(getTable());

		for (TableInputDescriptor input : getInputs()) {
			validityHashBuilder.append(input.hashCode());
		}

		for (TableInputDescriptor input : getInputs()) {
			final long length = Preprocessor.resolveSourceFile(input.getSourceFile(), csvDirectory, tag).length();
			validityHashBuilder.append(length);
		}

		return validityHashBuilder.toHashCode();
	}
}
