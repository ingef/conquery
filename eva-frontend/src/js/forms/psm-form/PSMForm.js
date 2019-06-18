// @flow

import React from "react";
import { Field, reduxForm } from "redux-form";

import {
  validateRequired,
  validatePositive
} from "../../external-forms/validators";

import { selectReduxFormState } from "../../external-forms/stateSelectors";

import { FormConceptGroup } from "../../external-forms/form-concept-group";
import { FormQueryDropzone } from "../../external-forms/form-query-dropzone";
import ExternalFormsHeader from "../../external-forms/ExternalFormsHeader";

import { CONCEPT_MATCHING_TYPES, PSM } from "../psm-form/conceptMatchingTypes";

import { POSITIVE_NUMBERS_PATTERN } from "../common/NumberPatterns";
import FormField from "../common/FormField";

import { type } from "./formType";

import { T } from "conquery/lib/js/localization";
import type { SelectOptionsT } from "conquery/lib/js/api/types";

import InputText from "conquery/lib/js/form-components/InputText";
import InputSelect from "conquery/lib/js/form-components/InputSelect";
import InputCheckbox from "conquery/lib/js/form-components/InputCheckbox";

const INDEX_DATE_OPTIONS = ["BEFORE", "NEUTRAL", "AFTER"];
const TIME_UNIT_OPTIONS = ["DAYS", "QUARTERS"];
const QUERY_GROUP_TIMESTAMP_OPTIONS = ["FIRST", "LAST", "RANDOM"];

type PropsType = {
  onSubmit: Function,
  availableDatasets: SelectOptionsT,
  selectedDatasetId: string
};

const setMatchingType = (value, valueIdx, matchingType) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    matchingType
  },
  ...value.slice(valueIdx + 1)
];

const isValidConcept = (item: Object): boolean =>
  item.ids &&
  !["geburtsdatum_export"].some(id =>
    item.ids.some(conceptId => conceptId.startsWith(id.toLowerCase()))
  );

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const ConceptGroup = FormField(FormConceptGroup);
const Select = FormField(InputSelect);
const Checkbox = FormField(InputCheckbox);

const PSMForm = (props: PropsType) => {
  return (
    <form>
      <ExternalFormsHeader
        headline={T.translate("externalForms.psmForm.headline")}
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
        name="controlGroupTimestamp"
        component={Select}
        props={{
          label: T.translate("externalForms.psmForm.controlGroupTimestamp"),
          options: QUERY_GROUP_TIMESTAMP_OPTIONS.map(value => ({
            label: T.translate(
              `externalForms.common.timestamp.${value.toLowerCase()}`
            ),
            value
          }))
        }}
      />
      <Field
        name="controlGroupDataset"
        component={Select}
        props={{
          label: T.translate("externalForms.psmForm.controlGroupDataset"),
          options: props.availableDatasets
        }}
      />
      <Field
        name="controlGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.psmForm.controlGroup"),
          dropzoneText: T.translate("externalForms.psmForm.resultGroupDropzone")
        }}
      />
      <Field
        name="featureGroupTimestamp"
        component={Select}
        props={{
          label: T.translate("externalForms.psmForm.featureGroupTimestamp"),
          options: QUERY_GROUP_TIMESTAMP_OPTIONS.map(value => ({
            label: T.translate(
              `externalForms.common.timestamp.${value.toLowerCase()}`
            ),
            value
          }))
        }}
      />
      <Field
        name="featureGroupDataset"
        component={Select}
        props={{
          label: T.translate("externalForms.psmForm.featureGroupDataset"),
          options: props.availableDatasets
        }}
      />
      <Field
        name="featureGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.psmForm.featureGroup"),
          dropzoneText: T.translate("externalForms.psmForm.resultGroupDropzone")
        }}
      />
      <Field
        name="timeUnit"
        component={Select}
        props={{
          label: T.translate("externalForms.psmForm.timeUnit.label"),
          options: TIME_UNIT_OPTIONS.map(value => ({
            label: T.translate(
              `externalForms.common.timeUnit.${value.toLowerCase()}`
            ),
            value
          }))
        }}
      />
      <Field
        name="timeCountBefore"
        component={Text}
        props={{
          inputType: "number",
          placeholder: "-",
          label: T.translate("externalForms.psmForm.timeCountBefore"),
          inputProps: {
            pattern: POSITIVE_NUMBERS_PATTERN
          }
        }}
      />
      <Field
        name="timeCountAfter"
        component={Text}
        props={{
          inputType: "number",
          placeholder: "-",
          label: T.translate("externalForms.psmForm.timeCountAfter"),
          inputProps: {
            pattern: POSITIVE_NUMBERS_PATTERN
          }
        }}
      />
      <Field
        name="indexDate"
        component={Select}
        props={{
          label: T.translate("externalForms.common.indexDate.label"),
          options: INDEX_DATE_OPTIONS.map(value => ({
            label: T.translate(
              `externalForms.common.indexDate.${value.toLowerCase()}`
            ),
            value
          }))
        }}
      />
      <Field
        name="automaticVariableSelection"
        component={Checkbox}
        props={{
          label: T.translate("externalForms.psmForm.automaticVariableSelection")
        }}
      />
      <Field
        name="features"
        component={ConceptGroup}
        props={{
          name: "features",
          label: T.translate("externalForms.psmForm.feature"),
          conceptDropzoneText: T.translate(
            "externalForms.psmForm.attributeDropzone"
          ),
          attributeDropzoneText: T.translate(
            "externalForms.psmForm.attributeDropzone"
          ),
          datasetId: props.selectedDatasetId,
          formType: type,
          newValue: {
            matchingType: PSM,
            concepts: []
          },
          enableDropFile: true,
          renderRowPrefix: (input, feature, i) => (
            <InputSelect
              label={T.translate("externalForms.forms")}
              options={Object.keys(CONCEPT_MATCHING_TYPES).map(key => ({
                label: T.translate(
                  `externalForms.psmForm.conceptMatchingType.${key.toLowerCase()}`
                ),
                value: key
              }))}
              input={{
                value: feature.matchingType,
                onChange: value =>
                  input.onChange(setMatchingType(input.value, i, value))
              }}
              selectProps={{
                clearable: false,
                autosize: true,
                searchable: false
              }}
            />
          ),
          isValidConcept
        }}
      />
      <Field
        name="outcomes"
        component={ConceptGroup}
        props={{
          name: "outcomes",
          label: T.translate("externalForms.psmForm.outcome"),
          conceptDropzoneText: T.translate(
            "externalForms.psmForm.attributeDropzone"
          ),
          attributeDropzoneText: T.translate(
            "externalForms.psmForm.attributeDropzone"
          ),
          datasetId: props.selectedDatasetId,
          formType: type,
          enableDropFile: true,
          isValidConcept
        }}
      />
      <Field
        name="caliper"
        component={Text}
        props={{
          inputType: "number",
          label: T.translate("externalForms.psmForm.caliper"),
          inputProps: {
            step: "0.1",
            min: 0.0,
            max: 2.0
          },
          placeholder: "-"
        }}
      />
      <Field
        name="matchingPartners"
        component={Text}
        props={{
          inputType: "number",
          label: T.translate("externalForms.psmForm.matchingPartners"),
          placeholder: "-"
        }}
      />
      <Field
        name="excludeOutliersDead"
        component={Checkbox}
        props={{
          className: "psm-form__exclude-outliers-dead",
          label: T.translate("externalForms.psmForm.excludeOutliersDead")
        }}
      />
      <Field
        name="excludeOutliersMaxMoney"
        component={Text}
        props={{
          inputType: "number",
          label: T.translate("externalForms.psmForm.excludeOutliersMaxMoney")
        }}
      />
    </form>
  );
};

export default reduxForm({
  form: type,
  getFormState: selectReduxFormState,
  initialValues: {
    features: [],
    outcomes: [],
    controlGroup: null,
    controlGroupTimestamp: "FIRST",
    featureGroup: null,
    featureGroupTimestamp: "FIRST",
    indexDate: INDEX_DATE_OPTIONS[0],
    timeUnit: TIME_UNIT_OPTIONS[1],
    timeCountBefore: 1,
    timeCountAfter: 1,
    automaticVariableSelection: false,
    matchingPartners: 1,
    caliper: 0.2
  },
  destroyOnUnmount: false,
  validate: values => ({
    controlGroup: validateRequired(values.controlGroup),
    featureGroup: validateRequired(values.featureGroup),
    controlGroupTimestamp: validateRequired(values.controlGroupTimestamp),
    featureGroupTimestamp: validateRequired(values.featureGroupTimestamp),
    timeUnit: validateRequired(values.timeUnit),
    timeCountBefore:
      validateRequired(values.timeCountBefore) ||
      validatePositive(values.timeCountBefore),
    timeCountAfter:
      validateRequired(values.timeCountAfter) ||
      validatePositive(values.timeCountAfter),
    features: validateRequired(values.features),
    outcomes: validateRequired(values.outcomes),
    matchingPartners: validateRequired(values.matchingPartners),
    calipher: validateRequired(values.calipher),
    controlGroupDataset: validateRequired(values.controlGroupDataset),
    featureGroupDataset: validateRequired(values.featureGroupDataset)
  })
})(PSMForm);
