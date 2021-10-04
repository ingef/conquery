import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";
import { Field as RxFormField } from "redux-form";

import type { SelectOptionT } from "../../api/types";
import { nodeIsInvalid } from "../../model/node";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputDateRange from "../../ui-components/InputDateRange";
import InputPlain from "../../ui-components/InputPlain";
import InputSelect from "../../ui-components/InputSelect";
import ToggleButton from "../../ui-components/ToggleButton";
import FormField from "../common/FormField";
import type { GeneralField } from "../config-types";
import { Description } from "../form-components/Description";
import { Headline } from "../form-components/Headline";
import FormConceptGroup from "../form-concept-group/FormConceptGroup";
import {
  FormQueryDropzone,
  FormMultiQueryDropzone,
} from "../form-query-dropzone";
import FormTabNavigation from "../form-tab-navigation/FormTabNavigation";
import { isFormField, isOptionalField } from "../helper";

const TabsField = styled("div")``;

// Pre-set field components to avoid re-rendering,
// => Avoids losing input focus.
const Plain = FormField(InputPlain);
const QueryDropzone = FormField(FormQueryDropzone);
const MultiQueryDropzone = FormField(FormMultiQueryDropzone);
const DateRange = FormField(InputDateRange);
const ConceptGroup = FormField(FormConceptGroup);
const Select = FormField(InputSelect);
const Checkbox = FormField(InputCheckbox);
const Tabs = FormField(TabsField);

const SxToggleButton = styled(ToggleButton)`
  margin-bottom: 5px;
`;

const NestedFields = styled("div")`
  padding: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
`;

interface PropsT {
  formType: string;
  field: GeneralField;
  getFieldValue: () => any;
  locale: "de" | "en";
  availableDatasets: SelectOptionT[];
  optional?: boolean;
}

const Field = ({ field, ...commonProps }: PropsT) => {
  const {
    formType,
    optional,
    locale,
    availableDatasets,
    getFieldValue,
  } = commonProps;
  const { t } = useTranslation();

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
          component={Plain}
          props={{
            inputType: "text",
            label: field.label[locale],
            placeholder: (field.placeholder && field.placeholder[locale]) || "",
            fullWidth: field.style ? field.style.fullWidth : false,
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
          }}
        />
      );
    case "NUMBER":
      return (
        <RxFormField
          name={field.name}
          component={Plain}
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
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
            infoTooltip: field.tooltip ? field.tooltip[locale] : undefined,
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
          }}
        />
      );
    case "DATASET_SELECT":
      const defaultValue =
        availableDatasets.length > 0 ? availableDatasets[0].value : undefined;

      return (
        <RxFormField
          name={field.name}
          component={Select}
          defaultValue={defaultValue}
          props={{
            label: field.label[locale],
            options: availableDatasets,
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            optional,
          }}
        />
      );
    case "TABS":
      const tabToShow = field.tabs.find((tab) => tab.name === getFieldValue());

      return (
        <Tabs>
          <RxFormField
            name={field.name}
            component={FormTabNavigation}
            props={{
              options: field.tabs.map((tab) => ({
                label: tab.title[locale],
                value: tab.name,
                tooltip: tab.tooltip ? tab.tooltip[locale] : undefined,
              })),
            }}
          />
          {tabToShow && tabToShow.fields.length > 0 && (
            <NestedFields>
              {tabToShow.fields.map((f, i) => {
                const key = isFormField(f) ? f.name : f.type + i;
                const nestedFieldOptional = isOptionalField(f);

                return (
                  <Field
                    key={key}
                    field={f}
                    {...commonProps}
                    optional={nestedFieldOptional}
                  />
                );
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
            tooltip: field.tooltip ? field.tooltip[locale] : undefined,
            conceptDropzoneText: field.conceptDropzoneLabel
              ? field.conceptDropzoneLabel[locale]
              : t("externalForms.default.conceptDropzoneLabel"),
            attributeDropzoneText: field.conceptColumnDropzoneLabel
              ? field.conceptColumnDropzoneLabel[locale]
              : t("externalForms.default.conceptDropzoneLabel"),
            formType,
            enableDropFile: true,
            disallowMultipleColumns: !field.isTwoDimensional,
            isSingle: field.isSingle,
            blocklistedTables: field.blocklistedConnectors,
            allowlistedTables: field.allowlistedConnectors,
            defaults: field.defaults,
            optional,
            isValidConcept: (item: Object) =>
              !nodeIsInvalid(
                item,
                field.blocklistedConceptIds,
                field.allowlistedConceptIds,
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
                  [field.rowPrefixField.name]:
                    field.rowPrefixField.defaultValue,
                }
              : { concepts: [], connector: "OR" },
            renderRowPrefix:
              field.rowPrefixField &&
              ((input, feature, i) => (
                <SxToggleButton
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
