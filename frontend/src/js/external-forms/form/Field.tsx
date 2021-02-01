import React from "react";
import styled from "@emotion/styled";
import { Field as RxFormField } from "redux-form";

import { T } from "../../localization";
import { nodeIsInvalid } from "../../model/node";

import InputSelect from "../../form-components/InputSelect";
import InputText from "../../form-components/InputText";
import InputDateRange from "../../form-components/InputDateRange";
import InputCheckbox from "../../form-components/InputCheckbox";

import type { DatasetIdT, SelectOptionT } from "../../api/types";

import {
  FormQueryDropzone,
  FormMultiQueryDropzone,
} from "../form-query-dropzone";
import FormConceptGroup from "../form-concept-group/FormConceptGroup";

import type { GeneralField } from "../config-types";

import FormField from "../common/FormField";
import { isFormField } from "../helper";
import FormTabNavigation from "../form-tab-navigation/FormTabNavigation";

import { Headline } from "../form-components/Headline";
import { Description } from "../form-components/Description";

const TabsField = styled("div")``;

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Text = FormField(InputText);
const QueryDropzone = FormField(FormQueryDropzone);
const MultiQueryDropzone = FormField(FormMultiQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);
const Select = FormField(InputSelect);
const Checkbox = FormField(InputCheckbox);
const Tabs = FormField(TabsField);

const SxInputSelect = styled(InputSelect)`
  margin-right: 5px;
  flex-grow: 1;
  max-width: 125px;
`;

const NestedFields = styled("div")`
  padding: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
`;

interface PropsT {
  formType: string;
  field: GeneralField;
  getFieldValue: (fieldName: string) => any;
  locale: "de" | "en";
  availableDatasets: SelectOptionT;
  selectedDatasetId: DatasetIdT;
}

const Field = ({ field, ...commonProps }: PropsT) => {
  const {
    formType,
    locale,
    availableDatasets,
    selectedDatasetId,
    getFieldValue,
  } = commonProps;

  switch (field.type) {
    case "HEADLINE":
      return <Headline>{field.label[locale]}</Headline>;
    case "DESCRIPTION":
      return (
        <Description
          dangerouslySetInnerHTML={{ __html: field.label[locale] || "" }}
        />
      );
    case "STRING":
      return (
        <RxFormField
          name={field.name}
          component={Text}
          props={{
            inputType: "text",
            label: field.label[locale],
            placeholder: (field.placeholder && field.placeholder[locale]) || "",
          }}
        />
      );
    case "NUMBER":
      return (
        <RxFormField
          name={field.name}
          component={Text}
          props={{
            inputType: "number",
            label: field.label[locale],
            placeholder: (field.placeholder && field.placeholder[locale]) || "",
            inputProps: {
              step: field.step || "1",
              pattern: field.pattern,
              min: field.min,
              max: field.max,
            },
          }}
        />
      );
    case "DATE_RANGE":
      return (
        <RxFormField
          name={field.name}
          component={DateRange}
          props={{
            inline: true,
            label: field.label[locale],
          }}
        />
      );
    case "RESULT_GROUP":
      return (
        <RxFormField
          name={field.name}
          component={QueryDropzone}
          props={{
            label: field.label[locale],
            dropzoneText: field.dropzoneLabel[locale],
          }}
        />
      );
    case "MULTI_RESULT_GROUP":
      return (
        <RxFormField
          name={field.name}
          component={MultiQueryDropzone}
          props={{
            label: field.label[locale],
            dropzoneChildren: () => field.dropzoneLabel[locale],
          }}
        />
      );
    case "CHECKBOX":
      return (
        <RxFormField
          name={field.name}
          component={Checkbox}
          props={{
            label: field.label[locale],
          }}
        />
      );
    case "SELECT":
      return (
        <RxFormField
          name={field.name}
          component={Select}
          props={{
            label: field.label[locale],
            options: field.options.map((option) => ({
              label: option.label[locale],
              value: option.value,
            })),
          }}
        />
      );
    case "DATASET_SELECT":
      return (
        <RxFormField
          name={field.name}
          component={Select}
          props={{
            label: field.label[locale],
            options: availableDatasets,
          }}
        />
      );
    case "TABS":
      const tabToShow = field.tabs.find(
        (tab) => tab.name === getFieldValue(field.name)
      );

      return (
        <Tabs>
          <RxFormField
            name={field.name}
            component={FormTabNavigation}
            props={{
              options: field.tabs.map((tab) => ({
                label: tab.title[locale],
                value: tab.name,
              })),
            }}
          />
          {tabToShow && tabToShow.fields.length > 0 && (
            <NestedFields>
              {tabToShow.fields.map((f, i) => {
                const key = isFormField(f) ? f.name : f.type + i;

                return <Field key={key} field={f} {...commonProps} />;
              })}
            </NestedFields>
          )}
        </Tabs>
      );
    case "CONCEPT_LIST":
      return (
        <RxFormField
          name={field.name}
          component={ConceptGroup}
          props={{
            fieldName: field.name,
            label: field.label[locale],
            conceptDropzoneText: field.conceptDropzoneLabel
              ? field.conceptDropzoneLabel[locale]
              : T.translate("externalForms.default.conceptDropzoneLabel"),
            attributeDropzoneText: field.conceptColumnDropzoneLabel
              ? field.conceptColumnDropzoneLabel[locale]
              : T.translate("externalForms.default.conceptDropzoneLabel"),
            datasetId: selectedDatasetId,
            formType,
            enableDropFile: true,
            disallowMultipleColumns: !field.isTwoDimensional,
            isSingle: field.isSingle,
            blacklistedTables: field.blacklistedConnectors,
            whitelistedTables: field.whitelistedConnectors,
            defaults: field.defaults,
            isValidConcept: (item: Object) =>
              !nodeIsInvalid(
                item,
                field.blacklistedConceptIds,
                field.whitelistedConceptIds
              ),
            // What follows is VERY custom
            // Concept Group supports rendering a prefix field
            // That's specifically required by one of the forms: "PSM Form"
            // So the following looks like it wants to be generic,
            // but it's really implemented for one field
            newValue: field.rowPrefixField
              ? {
                  concepts: [],
                  connector: "OR",
                  type: field.rowPrefixField.apiType,
                  [field.rowPrefixField.name]:
                    field.rowPrefixField.defaultValue,
                }
              : { concepts: [], connector: "OR" },
            renderRowPrefix:
              field.rowPrefixField &&
              ((input, feature, i) => (
                <SxInputSelect
                  options={field.rowPrefixField.options.map((option) => ({
                    label: option.label[locale],
                    value: option.value,
                  }))}
                  input={{
                    value: feature[field.rowPrefixField.name],
                    onChange: (value) =>
                      input.onChange([
                        ...input.value.slice(0, i),
                        {
                          ...input.value[i],
                          [field.rowPrefixField.name]: value,
                        },
                        ...input.value.slice(i + 1),
                      ]),
                  }}
                  selectProps={{
                    clearable: false,
                    autosize: true,
                    searchable: false,
                  }}
                />
              )),
          }}
        />
      );
    default:
      return null;
  }
};

export default Field;
