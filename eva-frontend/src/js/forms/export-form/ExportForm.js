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

const INDEX_DATE_OPTIONS = ["BEFORE", "NEUTRAL", "AFTER"];
const TIME_UNIT_OPTIONS = ["DAYS", "QUARTERS"];
const QUERY_GROUP_TIMESTAMP_OPTIONS = ["FIRST", "LAST", "RANDOM"];
const TIME_MODE = ["ABSOLUTE", "RELATIVE"];

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);
const ToggleBtn = FormField(ToggleButton);
const Select = FormField(InputSelect);

const ExportForm = (props: PropsType) => {
  return (
    <form>
      <ExternalFormsHeader
        headline={T.translate("externalForms.exportForm.headline")}
      />
      <Field
        name="queryGroup"
        component={QueryDropzone}
        props={{
          label: T.translate("externalForms.exportForm.queryGroup"),
          dropzoneText: T.translate(
            "externalForms.exportForm.queryGroupDropzone"
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
                `externalForms.exportForm.timeMode.${value.toLowerCase()}`
              ),
              value
            }))
          }}
        />
      }
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
            name="outcomes"
            component={ConceptGroup}
            props={{
              name: "outcomes",
              label: T.translate("externalForms.exportForm.concepts"),
              conceptDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
              enableDropFile: true
            }}
          />
        </div>
      )}
      {props.isRelativeTimeMode && (
        <div>
          <Field
            name="timeUnit"
            component={InputSelect}
            props={{
              label: T.translate("externalForms.exportForm.timeUnit.label"),
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
              placeholder: "0",
              label: T.translate("externalForms.exportForm.timeCountBefore"),
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
              label: T.translate("externalForms.exportForm.timeCountAfter"),
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
            name="features"
            component={ConceptGroup}
            props={{
              name: "features",
              label: T.translate("externalForms.exportForm.feature"),
              conceptDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
              enableDropFile: true
            }}
          />
          <Field
            name="outcomes"
            component={ConceptGroup}
            props={{
              name: "outcomes",
              label: T.translate("externalForms.exportForm.outcome"),
              conceptDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              attributeDropzoneText: T.translate(
                "externalForms.exportForm.attributeDropzone"
              ),
              datasetId: props.selectedDatasetId,
              formType: type,
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
      timeUnit: "QUARTERS",
      indexDate: "BEFORE",
      timestamp: "FIRST",
      dateRange: {
        min: null,
        max: null
      }
    },
    destroyOnUnmount: false,
    validate: values => {
      const errors: Object = {
        queryGroup: validateRequired(values.queryGroup),
        outcomes:
          validateRequired(values.outcomes) ||
          validateConceptGroupFilled(values.outcomes)
      };

      if (isRelativeTimeMode(values.timeMode)) {
        errors["outcomes"] =
          validateRequired(values.outcomes) ||
          validateConceptGroupFilled(values.outcomes);
        errors["timeCountBefore"] =
          validateRequired(values.timeCountBefore) ||
          validatePositive(values.timeCountBefore);
        errors["timeCountAfter"] =
          validateRequired(values.timeCountAfter) ||
          validatePositive(values.timeCountAfter);
        errors["timeUnit"] = validateRequired(values.timeUnit);
        errors["indexDate"] = validateRequired(values.indexDate);
        errors["timestamp"] = validateRequired(values.timestamp);
      } else {
        errors["dateRange"] = validateDateRange(values.dateRange);
      }

      return errors;
    }
  })(ExportForm)
);
