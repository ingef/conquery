import styled from "@emotion/styled";
import { memo, ReactNode } from "react";
import {
  Control,
  ControllerRenderProps,
  useController,
  UseFormRegister,
  UseFormSetValue,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { DateStringMinMax } from "../../common/helpers";
import { exists } from "../../common/helpers/exists";
import { nodeIsInvalid } from "../../model/node";
import { DragItemQuery } from "../../standard-query-editor/types";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputDateRange from "../../ui-components/InputDateRange";
import InputPlain from "../../ui-components/InputPlain/InputPlain";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import ToggleButton from "../../ui-components/ToggleButton";
import type { FormField, GeneralField } from "../config-types";
import { Description } from "../form-components/Description";
import { Headline } from "../form-components/Headline";
import FormConceptGroup, {
  FormConceptGroupT,
} from "../form-concept-group/FormConceptGroup";
import {
  FormQueryDropzone,
  FormMultiQueryDropzone,
} from "../form-query-dropzone";
import FormTabNavigation from "../form-tab-navigation/FormTabNavigation";
import { getInitialValue, isFormField, isOptionalField } from "../helper";
import { getErrorForField } from "../validators";

import type { DynamicFormValues } from "./Form";

type Props<T> = T & {
  children: (props: ControllerRenderProps<DynamicFormValues> & T) => ReactNode;
  control: Control<DynamicFormValues>;
  formField: FormField;
  defaultValue?: any;
};
const FieldContainer = styled("div")`
  margin: 0 0 10px;
`;
const ConnectedField = <T extends Object>({
  children,
  control,
  formField,
  defaultValue,
  ...props
}: Props<T>) => {
  const { t } = useTranslation();
  const { field } = useController<DynamicFormValues>({
    name: formField.name,
    defaultValue,
    control,
    rules: {
      validate: (value) => getErrorForField(t, formField, value) || true,
    },
  });

  return <FieldContainer>{children({ ...field, ...props })}</FieldContainer>;
};

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
  locale: "de" | "en";
  availableDatasets: SelectOptionT[];
  optional?: boolean;
  register: UseFormRegister<DynamicFormValues>;
  setValue: UseFormSetValue<DynamicFormValues>;
  control: Control<DynamicFormValues>;
}

const setValueConfig = {
  shouldValidate: true,
  shouldDirty: true,
  shouldTouch: true,
};

const Field = ({ field, ...commonProps }: PropsT) => {
  const { formType, optional, locale, availableDatasets, setValue, control } =
    commonProps;
  const { t } = useTranslation();
  const defaultValue = isFormField(field)
    ? getInitialValue(field, { availableDatasets })
    : null;

  switch (field.type) {
    case "HEADLINE":
      return <Headline>{field.label[locale]}</Headline>;
    case "DESCRIPTION":
      return (
        <Description
          dangerouslySetInnerHTML={{ __html: field.label[locale] || "" }}
        />
      );
    // case "STRING":
    //   return (
    //     <RxFormField
    //       name={field.name}
    //       component={Plain}
    //       props={{
    //         inputType: "text",
    //         label: field.label[locale],
    //         placeholder: (field.placeholder && field.placeholder[locale]) || "",
    //         fullWidth: field.style ? field.style.fullWidth : false,
    //         tooltip: field.tooltip ? field.tooltip[locale] : undefined,
    //         optional,
    //       }}
    //     />
    //   );
    case "NUMBER":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputPlain
              ref={ref}
              inputType="number"
              label={field.label[locale] || ""}
              placeholder={
                (field.placeholder && field.placeholder[locale]) || ""
              }
              value={fieldProps.value as number | null}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
              inputProps={{
                step: field.step || "1",
                pattern: field.pattern,
                min: field.min,
                max: field.max,
              }}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
            />
          )}
        </ConnectedField>
      );
    case "DATE_RANGE":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => {
            return (
              <InputDateRange
                inline={true}
                label={field.label[locale]}
                tooltip={field.tooltip ? field.tooltip[locale] : undefined}
                optional={optional}
                value={fieldProps.value as DateStringMinMax}
                onChange={(value) =>
                  setValue(field.name, value, setValueConfig)
                }
              />
            );
          }}
        </ConnectedField>
      );
    case "RESULT_GROUP":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <FormQueryDropzone
              label={field.label[locale] || ""}
              dropzoneText={field.dropzoneLabel[locale] || ""}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
              value={fieldProps.value as DragItemQuery}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
            />
          )}
        </ConnectedField>
      );
    case "MULTI_RESULT_GROUP":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <FormMultiQueryDropzone
              label={field.label[locale] || ""}
              dropzoneChildren={() => field.dropzoneLabel[locale]}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
              value={fieldProps.value as DragItemQuery[]}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
            />
          )}
        </ConnectedField>
      );
    case "CHECKBOX":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputCheckbox
              value={fieldProps.value as boolean}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
              label={field.label[locale] || ""}
              infoTooltip={field.tooltip ? field.tooltip[locale] : undefined}
            />
          )}
        </ConnectedField>
      );
    case "SELECT":
      return (
        <ConnectedField
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputSelect
              label={field.label[locale]}
              options={field.options.map((option) => ({
                label: option.label[locale] || "",
                value: option.value,
              }))}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
              value={fieldProps.value as SelectOptionT | null}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
            />
          )}
        </ConnectedField>
      );
    case "DATASET_SELECT":
      const datasetDefaultValue =
        availableDatasets.length > 0 ? availableDatasets[0].value : undefined;

      return (
        <ConnectedField formField={field} control={control}>
          {({ ref, ...fieldProps }) => (
            <InputSelect
              defaultValue={datasetDefaultValue}
              label={field.label[locale]}
              options={availableDatasets}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
              value={fieldProps.value as SelectOptionT | null}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
            />
          )}
        </ConnectedField>
      );
    case "TABS":
      return (
        <ConnectedField
          control={control}
          formField={field}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => {
            const tabToShow = field.tabs.find(
              (tab) => tab.name === fieldProps.value,
            );

            return (
              <>
                <FormTabNavigation
                  selectedTab={fieldProps.value as string}
                  onSelectTab={(tab) =>
                    setValue(field.name, tab, setValueConfig)
                  }
                  options={field.tabs.map((tab) => ({
                    label: tab.title[locale] || "",
                    value: tab.name,
                    tooltip: tab.tooltip ? tab.tooltip[locale] : undefined,
                  }))}
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
              </>
            );
          }}
        </ConnectedField>
      );
    case "CONCEPT_LIST":
      return (
        <ConnectedField
          control={control}
          formField={field}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <FormConceptGroup
              fieldName={field.name}
              value={fieldProps.value as FormConceptGroupT[]}
              onChange={(value) => setValue(field.name, value, setValueConfig)}
              label={field.label[locale] || ""}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              conceptDropzoneText={
                field.conceptDropzoneLabel
                  ? field.conceptDropzoneLabel[locale] || ""
                  : t("externalForms.default.conceptDropzoneLabel")
              }
              attributeDropzoneText={
                field.conceptColumnDropzoneLabel
                  ? field.conceptColumnDropzoneLabel[locale] || ""
                  : t("externalForms.default.conceptDropzoneLabel")
              }
              formType={formType}
              disallowMultipleColumns={!field.isTwoDimensional}
              isSingle={field.isSingle}
              blocklistedTables={field.blocklistedConnectors}
              allowlistedTables={field.allowlistedConnectors}
              blocklistedSelects={field.blocklistedSelects}
              allowlistedSelects={field.allowlistedSelects}
              defaults={field.defaults}
              optional={optional}
              isValidConcept={(item: Object) =>
                !nodeIsInvalid(
                  item,
                  field.blocklistedConceptIds,
                  field.allowlistedConceptIds,
                )
              }
              // What follows is VERY custom
              // Concept Group supports rendering a prefix field
              // That's specifically required by one of the forms: "PSM Form"
              // So the following looks like it wants to be generic,
              // but it's really implemented for one field
              newValue={
                field.rowPrefixField
                  ? {
                      concepts: [],
                      connector: "OR",
                      [field.rowPrefixField.name]:
                        field.rowPrefixField.defaultValue,
                    }
                  : { concepts: [], connector: "OR" }
              }
              renderRowPrefix={
                exists(field.rowPrefixField)
                  ? ({ value: fieldValue, onChange, row, i }) => (
                      <SxToggleButton
                        options={field.rowPrefixField!.options.map(
                          (option) => ({
                            label: option.label[locale] || "",
                            value: option.value,
                          }),
                        )}
                        value={row[field.rowPrefixField!.name]}
                        onChange={(value) =>
                          onChange([
                            ...fieldValue.slice(0, i),
                            {
                              ...fieldValue[i],
                              [field.rowPrefixField!.name]: value,
                            },
                            ...fieldValue.slice(i + 1),
                          ])
                        }
                      />
                    )
                  : undefined
              }
            />
          )}
        </ConnectedField>
      );
    default:
      return null;
  }
};

export default memo(Field);
