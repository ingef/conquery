package com.bakdata.conquery.models.identifiable;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter @Setter @NoArgsConstructor @EqualsAndHashCode(callSuper=true) @AllArgsConstructor
public abstract class Labeled<ID extends IId<? extends Labeled<? extends ID>>> extends NamedImpl<ID> {
	
	@NotEmpty @ToString.Include
	private String label;
	
	public Labeled(Labeled<ID> labelSource) {
		this.label=labelSource.getLabel();
		this.setName(labelSource.getName());
	}
	
	public Labeled(String name, String label) {
		super(name);
		this.label=label;
	}
	
	
	//if only label or name is given the rest is inferred
	public final void setLabel(String label) {
		this.label=label;
		if(this.getName()==null) {
			this.setName(Named.makeSafe(label));
		}
	}
	
	@Override
	public final void setName(String name) {
		super.setName(name);
		if(this.getLabel()==null) {
			this.label=name;
		}
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[label=" + label + ", name=" + getName() + "]";
	}

}
