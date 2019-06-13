// @flow

import React from "react";
import { Field, reduxForm, formValueSelector } from "redux-form";
import { connect } from "react-redux";

import {
  validateRequired,
  validateDateRange,
  validatePositive,
  validateConceptGroupFilled
} from "../../external-forms/validators";
import { selectReduxFormState } from "../../external-forms/stateSelectors";
import { FormQueryDropzone } from "../../external-forms/form-query-dropzone";
import { FormConceptGroup } from "../../external-forms/form-concept-group";
import ExternalFormsHeader from "../../external-forms/ExternalFormsHeader";

import { RESOLUTION_OPTIONS, INDEX_DATE_OPTIONS } from "../options";
import DISALLOWED_CONCEPT_ID_PREFIXES from "../map-form/disallowedConcepts";
import { mapOptionToLabelKey } from "../formHelper";

import { POSITIVE_NUMBERS_PATTERN } from "../common/NumberPatterns";
import FormField from "../common/FormField";

import { type } from "./formType";

import { T } from "conquery/lib/js/localization";

import InputSelect from "conquery/lib/js/form-components/InputSelect";
import InputText from "conquery/lib/js/form-components/InputText";
import InputDateRange from "conquery/lib/js/form-components/InputDateRange";
import ToggleButton from "conquery/lib/js/form-components/ToggleButton";

type PropsType = {
  onSubmit: Function,
  isRelativeTimeMode: boolean,
  selectedDatasetId: string
};

const QUERY_GROUP_TIMESTAMP_OPTIONS = ["FIRST", "LAST", "RANDOM"];
const TIME_MODE = ["ABSOLUTE", "RELATIVE"];
const EXTENDED_DISALLOWED_CONCEPT_ID_PREFIXES = [
  ...DISALLOWED_CONCEPT_ID_PREFIXES,
  "alter",
  "versichertentage"
];

const isValidConcept = (item: Object): boolean =>
  item.ids &&
  !EXTENDED_DISALLOWED_CONCEPT_ID_PREFIXES.some(id =>
    item.ids.some(conceptId => conceptId.startsWith(id.toLowerCase()))
  );

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);
const ToggleBtn = FormField(ToggleButton);
const Select = FormField(InputSelect);

const DescriptionForm = (props: PropsType) => {
  return (
    <form>
      <ExternalFormsHeader
        headline={T.translate("externalForms.descriptionForm.headline")}
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
        name="queryGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.common.queryGroup"),
          dropzoneText: T.translate(
            "externalForms.descriptionForm.queryGroupDropzoneText"
          )
        }}
      />
      {
        <Field
          name="timeMode"
          component={ToggleBtn}
          props={{
            options: TIME_MODE.map(value => ({
              label: T.translate(
                `externalForms.descriptionForm.timeMode.${value.toLowerCase()}`
              ),
              value
            }))
          }}
        />
      }
      <Field
        name="resolution"
        component={Select}
        props={{
          label: T.translate("externalForms.descriptionForm.resolution"),
          options: RESOLUTION_OPTIONS.map(value => ({
            label: T.translate(
              `externalForms.common.timeUnit.${mapOptionToLabelKey(
                value.toLowerCase()
              )}`
            ),
            value
          }))
        }}
      />
      {!props.isRelativeTimeMode && (
        <div>
          <Field
            name="dateRange"
            component={DateRange}
            props={{
              inline: true,
              label: T.translate("externalForms.common.timespan")
            }}
          />
          <Field
            name="features"
            component={ConceptGroup}
            props={{
              name: "features",
              label: T.translate("externalForms.descriptionForm.concepts"),
              conceptDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
              isValidConcept,
              enableDropFile: true
            }}
          />
        </div>
      )}
      {props.isRelativeTimeMode && (
        <div>
          <Field
            name="timeCountBefore"
            component={Text}
            props={{
              inputType: "number",
              placeholder: "0",
              label: T.translate(
                "externalForms.descriptionForm.timeCountBefore"
              ),
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
              placeholder: "0",
              label: T.translate(
                "externalForms.descriptionForm.timeCountAfter"
              ),
              inputProps: {
                pattern: POSITIVE_NUMBERS_PATTERN
              }
            }}
          />
          <Field
            name="timestamp"
            component={Select}
            props={{
              label: T.translate("externalForms.common.timestamp.label"),
              options: QUERY_GROUP_TIMESTAMP_OPTIONS.map(value => ({
                label: T.translate(
                  `externalForms.common.timestamp.${value.toLowerCase()}`
                ),
                value
              }))
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
            name="features"
            component={ConceptGroup}
            props={{
              name: "features",
              label: T.translate("externalForms.descriptionForm.features"),
              conceptDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
              isValidConcept,
              enableDropFile: true
            }}
          />
          <Field
            name="outcomes"
            component={ConceptGroup}
            props={{
              name: "outcomes",
              label: T.translate("externalForms.descriptionForm.outcomes"),
              conceptDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.descriptionForm.dropzoneText"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
              isValidConcept,
              enableDropFile: true
            }}
          />
        </div>
      )}
    </form>
  );
};

const isRelativeTimeMode = mode => mode === "RELATIVE";

const getField = formValueSelector(type, selectReduxFormState);

const mapStateToProps = state => ({
  isRelativeTimeMode: isRelativeTimeMode(getField(state, "timeMode"))
});

export default connect(mapStateToProps)(
  reduxForm({
    form: type,
    getFormState: selectReduxFormState,
    initialValues: {
      queryGroup: null,
      timeMode: "RELATIVE",
      timeCountBefore: 1,
      timeCountAfter: 1,
      features: [],
      outcomes: [],
      resolution: RESOLUTION_OPTIONS[0],
      timestamp: QUERY_GROUP_TIMESTAMP_OPTIONS[0],
      dateRange: {
        min: null,
        max: null
      },
      indexDate: INDEX_DATE_OPTIONS[0]
    },
    destroyOnUnmount: false,
    validate: values => {
      const errors: Object = {
        queryGroup: validateRequired(values.queryGroup),
        features:
          validateRequired(values.features) ||
          validateConceptGroupFilled(values.features)
      };

      if (isRelativeTimeMode(values.timeMode)) {
        errors["timeCountBefore"] =
          validateRequired(values.timeCountBefore) ||
          validatePositive(values.timeCountBefore);
        errors["timeCountAfter"] =
          validateRequired(values.timeCountAfter) ||
          validatePositive(values.timeCountAfter);
        errors["resolution"] = validateRequired(values.resolution);
        errors["indexDate"] = validateRequired(values.indexDate);
        errors["timestamp"] = validateRequired(values.timestamp);
        errors["outcomes"] =
          validateRequired(values.outcomes) ||
          validateConceptGroupFilled(values.outcomes);
      } else {
        errors["dateRange"] = validateDateRange(values.dateRange);
      }

      return errors;
    }
  })(DescriptionForm)
);
