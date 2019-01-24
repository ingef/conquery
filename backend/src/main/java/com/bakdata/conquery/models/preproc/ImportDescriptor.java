package com.bakdata.conquery.models.preproc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportDescriptorId;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ImportDescriptor extends Labeled<ImportDescriptorId> implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotEmpty
	private String table;
	@NotEmpty @Valid
	private Input[] inputs;
	
	@JsonIgnore
	private transient InputFile inputFile;
	
	@JsonIgnore
	@ValidationMethod(message="The output of each input needs the same number of output columns of the same type and name")
	public boolean isSameTypesInEachInput() {
		if(inputs.length==1) {
			return true;
		}
		List<MajorTypeId[]> types = new ArrayList<>();
		for(int i=0;i<inputs.length;i++) {
			MajorTypeId[] inp = Arrays
				.stream(inputs[i].getOutput())
				.map(Output::getResultType)
				.toArray(MajorTypeId[]::new);
			
			for(MajorTypeId[] o:types) {
				if(!Arrays.equals(inp, o)) {
					return false;
				}
			}
			types.add(inp);
		}
		return true;
	}
	
	public int calculateValidityHash() {
		HashCodeBuilder validityHashBuilder = new HashCodeBuilder()
				.append(this.getInputFile().getDescriptionFile().length());
		
		for(Input input:this.getInputs()) {
			validityHashBuilder
				.append(input.getSourceFile().length());
		}
		validityHashBuilder.append(15);
		return validityHashBuilder.toHashCode();
	}

	@Override
	public ImportDescriptorId createId() {
		return new ImportDescriptorId(getName());
	}

	@Override
	public String toString() {
		return "ImportDescriptor [table=" + table + ", name=" + getName() + ", file=" + getInputFile().getDescriptionFile() + "]";
	}
	
}
