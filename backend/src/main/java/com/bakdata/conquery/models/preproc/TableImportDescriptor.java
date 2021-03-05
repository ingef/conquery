package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.TableImportDescriptorId;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Combines potentially multiple input files to be loaded into a single table. Describing their respective transformation. All Inputs must produce the same types of outputs.
 *
 * For further detail see {@link TableInputDescriptor}, and {@link Preprocessor}.
 *
 * This file describes an `import.json` used as description for the `preprocess` command.
 */
@Getter
@Setter
@Slf4j
public class TableImportDescriptor extends Labeled<TableImportDescriptorId> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Target table to load the import to.
	 */
	@NotEmpty
	private String table;

	/**
	 * A single source input.
	 */
	@NotEmpty
	@Valid
	private TableInputDescriptor[] inputs;

	public static TableImportDescriptor read(File descriptionFile) throws IOException {
		return Jackson.MAPPER.readerFor(TableImportDescriptor.class)
							 .readValue(descriptionFile);
	}

	@JsonIgnore
	@ValidationMethod(message = "The output of each input needs the same number of output columns of the same type and name")
	public boolean isSameTypesInEachInput() {
		if (inputs.length == 1) {
			return true;
		}
		List<MajorTypeId[]> types = new ArrayList<>();

		for (TableInputDescriptor input : inputs) {
			MajorTypeId[] inp = Arrays.stream(input.getOutput())
									  .map(OutputDescription::getResultType)
									  .toArray(MajorTypeId[]::new);

			for (MajorTypeId[] out : types) {
				if (!Arrays.equals(inp, out)) {
					return false;
				}
			}
			types.add(inp);
		}
		return true;
	}

	@Override
	public TableImportDescriptorId createId() {
		return new TableImportDescriptorId(getName());
	}

	@Override
	public String toString() {
		return "ImportDescriptor [table=" + table + ", name=" + getName() + "]";
	}

	/**
	 * Calculate a hash of the descriptor. This is used to only recompute the import when files change.
	 * @param csvDirectory
	 * @param tag
	 */
	public int calculateValidityHash(Path csvDirectory, Optional<String> tag) throws IOException {
		HashCodeBuilder validityHashBuilder = new HashCodeBuilder();

		log.debug("name = {}", getName().hashCode());
		log.debug("table = {}", getTable().hashCode());

		validityHashBuilder
				.append(getName())
				.append(getTable())
		;

		for (TableInputDescriptor input : getInputs()) {
			log.debug("Input[{}] = {}", input.getSourceFile(), input.hashCode());

			validityHashBuilder.append(input.hashCode());
		}

		for (TableInputDescriptor input : getInputs()) {
			final long length = Preprocessor.resolveSourceFile(input.getSourceFile(), csvDirectory, tag).length();

			log.debug("File[{}] = {}", input.getSourceFile(), length);

			validityHashBuilder.append(length);
		}
		return validityHashBuilder.toHashCode();
	}

}
