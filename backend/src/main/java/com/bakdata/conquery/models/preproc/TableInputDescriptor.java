package com.bakdata.conquery.models.preproc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import groovy.lang.GroovyShell;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * An input describes transformations on a single CSV file to be loaded into the table described in {@link TableImportDescriptor}.
 *
 * It requires a primary Output and at least one normal output.
 *
 * Input data can be filter using the field filter, which is evaluated as a groovy script on every row.
 *
 */
@Data
@Slf4j
public class TableInputDescriptor {

	private static final long serialVersionUID = 1L;
	private static final String[] AUTO_IMPORTS = Stream.of(
			LocalDate.class,
			Range.class
	).map(Class::getName).toArray(String[]::new);

	@NotNull
	private String sourceFile;

	private String filter;

	/**
	 * Output producing the primary column. This should be the primary key across all tables.
	 * Default is `COPY("pid", STRING)`
	 */
	@NotNull
	@Valid
	private OutputDescription primary = new CopyOutput("pid", "id", MajorTypeId.STRING);

	@Valid @NotEmpty
	private OutputDescription[] output;

	/**
	 * Empty array to be used only for validation of groovy script.
	 */
	public static final String[] FAKE_HEADERS = new String[50];

	@JsonIgnore @ValidationMethod(message = "Groovy script is not valid.")
	public boolean isValidGroovyScript(){
		try{
			createFilter(FAKE_HEADERS);
		}
		catch (Exception ex) {
			log.error("Groovy script is not valid",ex);
			return false;
		}

		return true;
	}


	@JsonIgnore
	@ValidationMethod(message = "Each column requires a unique name")
	public boolean isEachNameUnique() {
		Object2IntMap<String> names = new Object2IntArrayMap<>(getWidth());
		names.defaultReturnValue(-1);

		for (int index = 0; index < output.length; index++) {
			int prev = names.put(output[index].getName(), index);
			if(prev != -1){
				log.error("Duplicate Output to Column[{}] at indices {} and {}", output[index].getName(), prev, index);
				return false;
			}
		}

		return true;
	}

	@JsonIgnore
	@ValidationMethod(message = "The primary column must be of type STRING")
	public boolean isPrimaryString() {
		return primary.getResultType() == MajorTypeId.STRING;
	}

	public GroovyPredicate createFilter(String[] headers){
		if(filter == null) {
			return null;
		}

		try {
			CompilerConfiguration config = new CompilerConfiguration();
			config.addCompilationCustomizers(new ImportCustomizer().addImports(AUTO_IMPORTS));
			config.setScriptBaseClass(GroovyPredicate.class.getName());

			GroovyShell groovy = new GroovyShell(config);

			for (int col = 0; col < headers.length; col++) {
				groovy.setVariable(headers[col], col);
			}

			return  (GroovyPredicate) groovy.parse(filter);
		} catch (Exception e) {
			throw new RuntimeException("Failed to compile filter `" + filter + "`", e);
		}
	}

	@JsonIgnore
	public int getWidth() {
		return getOutput().length;
	}

	public ColumnDescription getColumnDescription(int i) {
		return output[i].getColumnDescription();
	}


	/**
	 * Create a mapping from a header to it's column position.
	 */
	public static Object2IntArrayMap<String> buildHeaderMap(String[] headers) {
		final int[] indices = new int[headers.length];
		Arrays.setAll(indices, i -> i);
		return new Object2IntArrayMap<>(headers, indices);
	}


	public int hashCode() {
		final HashCodeBuilder builder = new HashCodeBuilder();

		builder.append(getSourceFile());
		builder.append(getFilter());
		builder.append(getPrimary());

		for (OutputDescription outputDescription : getOutput()) {
			builder.append(outputDescription.hashCode());
		}

		return builder.hashCode();
	}
}
