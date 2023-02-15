package com.bakdata.conquery.models.forms.managed;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal runtime representation of a form query.
 */
@Getter
@Setter
@ToString
@Slf4j
@EqualsAndHashCode(callSuper = true)
@CPSType(id = "MANAGED_FORM", base = ManagedExecution.class)
public class ManagedForm<F extends Form> extends ManagedExecution {

	/**
	 * The form that was submitted through the api.
	 */
	private F submittedForm;

	protected ManagedForm(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public ManagedForm(F submittedForm, User owner, Dataset submittedDataset, MetaStorage storage) {
		super(owner, submittedDataset, storage);
		this.submittedForm = submittedForm;
	}

	@Override
	protected void doInitExecutable(Namespace namespace, ConqueryConfig config) {

	}

	@Override
	public void start() {
		synchronized (this) {
			super.start();

			if (getSubmittedForm().getValues() != null) {
				// save as formConfig
				final FormConfigAPI build = FormConfigAPI.builder().formType(getSubmittedForm().getFormType())
														 .label(this.getLabelWithoutAutoLabelSuffix())
														 .tags(this.getTags())
														 .values(getSubmittedForm().getValues()).build();

				final FormConfig formConfig = build.intern(getOwner(), getDataset());

				getStorage().addFormConfig(formConfig);
			}
		}
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		submittedForm.visit(visitor);
	}


	@Override
	@JsonIgnore
	public QueryDescription getSubmitted() {
		return submittedForm;
	}




	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return getSubmittedForm().getLocalizedTypeLabel();
	}

}
