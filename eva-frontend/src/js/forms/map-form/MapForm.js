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
import { type ExternalFormPropsType } from "../../external-forms/types";

import { REGIONS, GRANULARITY_LEVELS, RESOLUTION_OPTIONS } from "../options";
import { mapOptionToLabelKey } from "../formHelper";
import FormField from "../common/FormField";

import DISALLOWED_CONCEPT_ID_PREFIXES from "./disallowedConcepts";
import { type } from "./formType";

import { T } from "conquery/lib/js/localization";

import InputSelect from "conquery/lib/js/form-components/InputSelect";
import InputDateRange from "conquery/lib/js/form-components/InputDateRange";
import InputText from "conquery/lib/js/form-components/InputText";
import InputCheckbox from "conquery/lib/js/form-components/InputCheckbox";

type PropsType = ExternalFormPropsType & {
  onSubmit: Function
};

const isValidConcept = (item: Object): boolean =>
  item.ids &&
  !DISALLOWED_CONCEPT_ID_PREFIXES.some(id =>
    item.ids.some(conceptId => conceptId.startsWith(id.toLowerCase()))
  );

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);
const Select = FormField(InputSelect);
const Checkbox = FormField(InputCheckbox);

const MapForm = (props: PropsType) => {
  return (
    <form>
      <ExternalFormsHeader
        headline={T.translate("externalForms.mapForm.headline")}
      />
      <Field
        name="title"
        component={Text}
        props={{
          inputType: "text",
          label: T.translate("common.title"),
          placeholder: T.translate("common.title")
        }}
      />
      <Field
        name="description"
        component={Text}
        props={{
          inputType: "text",
          label: T.translate("common.description"),
          fullWidth: true,
          placeholder: T.translate("common.description")
        }}
      />
      <Field
        name="region"
        component={Select}
        props={{
          label: T.translate("externalForms.mapForm.region"),
          options: REGIONS
        }}
      />
      <Field
        name="granularity"
        component={Select}
        props={{
          label: T.translate("externalForms.mapForm.granularity.label"),
          options: GRANULARITY_LEVELS.map(g => ({
            label: T.translate(
              `externalForms.mapForm.granularity.levels.${g.toLowerCase()}`
            ),
            value: g
          }))
        }}
      />
      <Field
        name="queryGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.common.queryGroup"),
          dropzoneText: T.translate("externalForms.psmForm.resultGroupDropzone")
        }}
      />
      <Field
        name="features"
        component={ConceptGroup}
        props={{
          name: "features",
          label: T.translate("externalForms.mapForm.features"),
          conceptDropzoneText: T.translate(
            "externalForms.mapForm.conceptNodeDropzoneText"
          ),
          attributeDropzoneText: T.translate(
            "externalForms.mapForm.conceptNodeDropzoneText"
          ),
          datasetId: props.selectedDatasetId,
          formType: type,
          isValidConcept,
          disallowMultipleColumns: true,
          enableDropFile: true
        }}
      />
      <Field
        name="isRelative"
        component={Checkbox}
        props={{
          label: T.translate("externalForms.mapForm.isRelative"),
          className: "map-form__map-mode-checkbox"
        }}
      />
      <Field
        name="resolution"
        component={Select}
        props={{
          label: T.translate("externalForms.mapForm.timeUnit.label"),
          options: RESOLUTION_OPTIONS.map(o => ({
            value: o,
            label: T.translate(
              `externalForms.common.timeUnit.${mapOptionToLabelKey(o)}`
            )
          }))
        }}
      />
      <Field
        name="dateRange"
        component={DateRange}
        props={{
          inline: true,
          label: T.translate("externalForms.common.timespan")
        }}
      />
    </form>
  );
};

export default reduxForm({
  form: type,
  getFormState: selectReduxFormState,
  initialValues: {
    dateRange: {},
    queryGroup: null,
    granularity: GRANULARITY_LEVELS[0],
    region: REGIONS[0].value,
    resolution: RESOLUTION_OPTIONS[0],
    features: []
  },
  destroyOnUnmount: false,
  validate: values => ({
    dateRange: validateDateRange(values.dateRange),
    queryGroup: validateRequired(values.queryGroup),
    granularity: validateRequired(values.granularity),
    region: validateRequired(values.region),
    resolution: validateRequired(values.resolution),
    features: validateRequired(values.features)
  })
})(MapForm);
