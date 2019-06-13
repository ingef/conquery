// @flow

import React from "react";
import { Field, reduxForm } from "redux-form";

import {
  validateRequired,
  validateDateRange
} from "../../external-forms/validators";
import { selectReduxFormState } from "../../external-forms/stateSelectors";

import { FormQueryDropzone } from "../../external-forms/form-query-dropzone";
import { FormConceptGroup } from "../../external-forms/form-concept-group";
import ExternalFormsHeader from "../../external-forms/ExternalFormsHeader";

import FormField from "../common/FormField";

import { type } from "./formType";

import { T } from "conquery/lib/js/localization";

import InputText from "conquery/lib/js/form-components/InputText";
import InputDateRange from "conquery/lib/js/form-components/InputDateRange";

type PropsType = {
  onSubmit: Function,
  selectedDatasetId: string
};

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);

const AUForm = (props: PropsType) => {
  return (
    <form>
      <ExternalFormsHeader
        headline={T.translate("externalForms.auForm.headline")}
      />
      <Field
        name="title"
        component={Text}
        props={{
          label: T.translate("common.title"),
          inputType: "text",
          placeholder: T.translate("common.title")
        }}
      />
      <Field
        name="description"
        component={Text}
        props={{
          label: T.translate("common.description"),
          inputType: "text",
          fullWidth: true,
          placeholder: T.translate("common.description")
        }}
      />
      <Field
        name="queryGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.common.queryGroup"),
          dropzoneText: T.translate(
            "externalForms.psmForm.resultGroupDropzone"
          ),
          className: "map-form__dropzone"
        }}
      />
      <Field
        name="dateRange"
        component={DateRange}
        props={{
          label: T.translate("externalForms.common.timespan"),
          inline: true
        }}
      />
      <Field
        name="baseCondition"
        component={ConceptGroup}
        props={{
          name: "baseCondition",
          label: T.translate("externalForms.auForm.baseCondition"),
          conceptDropzoneText: T.translate(
            "externalForms.auForm.attributeDropzone"
          ),
          attributeDropzoneText: T.translate(
            "externalForms.auForm.attributeDropzone"
          ),
          datasetId: props.selectedDatasetId,
          formType: type,
          enableDropFile: true,
          disallowMultipleColumns: true
        }}
      />
    </form>
  );
};

export default reduxForm({
  form: type,
  getFormState: selectReduxFormState,
  initialValues: {
    queryGroup: null,
    baseCondition: [],
    dateRange: {
      min: null,
      max: null
    }
  },
  destroyOnUnmount: false,
  validate: values => ({
    queryGroup: validateRequired(values.queryGroup),
    dateRange: validateDateRange(values.dateRange)
  })
})(AUForm);
