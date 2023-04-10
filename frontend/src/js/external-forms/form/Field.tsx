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
import type { DateStringMinMax } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { useDatasetId } from "../../dataset/selectors";
import type { Language } from "../../localization/useActiveLang";
import { nodeIsInvalid } from "../../model/node";
import type { DragItemQuery } from "../../standard-query-editor/types";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputDateRange from "../../ui-components/InputDateRange";
import InputPlain from "../../ui-components/InputPlain/InputPlain";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import { InputTextarea } from "../../ui-components/InputTextarea/InputTextarea";
import ToggleButton from "../../ui-components/ToggleButton";
import type { Field as FieldT, GeneralField, Tabs } from "../config-types";
import { Description } from "../form-components/Description";
import { getHeadlineFieldAs, Headline } from "../form-components/Headline";
import FormConceptGroup from "../form-concept-group/FormConceptGroup";
import type { FormConceptGroupT } from "../form-concept-group/formConceptGroupState";
import FormQueryDropzone from "../form-query-dropzone/FormQueryDropzone";
import FormTabNavigation from "../form-tab-navigation/FormTabNavigation";
import {
  getFieldKey,
  getInitialValue,
  getUniqueFieldname,
  isFormField,
  isOptionalField,
} from "../helper";
import { getErrorForField } from "../validators";

import type { DynamicFormValues } from "./Form";

const BOTTOM_MARGIN = 7;

// TODO: REFINE COLORS
// const useColorByField = (fieldType: FormField["type"]) => {
//   const theme = useTheme();

//   const COLOR_BY_FIELD_TYPE: Record<FormField["type"], string> = useMemo(
//     () => ({
//       STRING: theme.col.palette[8],
//       DATE_RANGE: theme.col.palette[0],
//       NUMBER: theme.col.palette[1],
//       CONCEPT_LIST: theme.col.palette[2],
//       SELECT: theme.col.palette[3],
//       DATASET_SELECT: theme.col.palette[4],
//       CHECKBOX: theme.col.palette[7],
//       RESULT_GROUP: theme.col.palette[5],
//       TABS: theme.col.palette[9],
//     }),
//     [theme],
//   );

//   return COLOR_BY_FIELD_TYPE[fieldType];
// };

type Props<T> = T & {
  children: (props: ControllerRenderProps<DynamicFormValues>) => ReactNode;
  control: Control<DynamicFormValues>;
  formType: string;
  formField: FieldT | Tabs;
  defaultValue?: any;
  noContainer?: boolean;
  noLabel?: boolean;
};
const FieldContainer = styled("div")<{ noLabel?: boolean }>`
  margin: 0 0 ${BOTTOM_MARGIN}px;
  padding: ${({ noLabel }) => (noLabel ? "7px 10px" : "2px 10px 7px")};
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
`;
const ConnectedField = <T extends Object>({
  children,
  control,
  formType,
  formField,
  defaultValue,
  noContainer,
  noLabel,
  ...props
}: Props<T>) => {
  const { t } = useTranslation();
  const { field } = useController<DynamicFormValues>({
    name: getUniqueFieldname(formType, formField),
    defaultValue,
    control,
    rules: {
      validate: (value) => getErrorForField(t, formField, value) || true,
    },
    shouldUnregister: false,
  });

  // TODO: REFINE COLORS
  // const color = useColorByField(formField.type);

  return noContainer ? (
    <div>{children({ ...field, ...props })}</div>
  ) : (
    <FieldContainer noLabel={noLabel}>
      {children({ ...field, ...props })}
    </FieldContainer>
  );
};

const SxToggleButton = styled(ToggleButton)`
  margin-bottom: 5px;
`;

const Spacer = styled("div")`
  margin-bottom: ${BOTTOM_MARGIN * 2}px;
`;

const Group = styled("div")`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`;

const NestedFields = styled("div")`
  padding: 12px 10px 5px;
  background-color: ${({ theme }) => theme.col.bg};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  border-radius: ${({ theme }) => theme.borderRadius};
  margin-bottom: ${BOTTOM_MARGIN * 2}px;
`;

interface PropsT {
  formType: string;
  field: GeneralField;
  locale: Language;
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
  const datasetId = useDatasetId();
  const { formType, optional, locale, availableDatasets, setValue, control } =
    commonProps;
  const { t } = useTranslation();

  const defaultValue =
    isFormField(field) && field.type !== "GROUP"
      ? getInitialValue(field, { availableDatasets, activeLang: locale })
      : null;

  const uniqueFieldname =
    isFormField(field) && field.type !== "GROUP"
      ? getUniqueFieldname(formType, field)
      : ""; // To avoid null checks. We won't use this value for non-form-fields and GROUP fields.

  switch (field.type) {
    case "HEADLINE":
      return (
        <Headline as={getHeadlineFieldAs(field)} size={field.style?.size}>
          {field.label[locale]}
        </Headline>
      );
    case "DESCRIPTION":
      return (
        <Description
          dangerouslySetInnerHTML={{ __html: field.label[locale] || "" }}
        />
      );
    case "STRING":
      return (
        <ConnectedField
          formType={formType}
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputPlain
              ref={ref}
              inputType="text"
              label={field.label[locale] || ""}
              placeholder={
                (field.placeholder && field.placeholder[locale]) || ""
              }
              fullWidth={field.style ? field.style.fullWidth : false}
              value={fieldProps.value as string}
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
            />
          )}
        </ConnectedField>
      );
    case "TEXTAREA":
      return (
        <ConnectedField
          formType={formType}
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputTextarea
              ref={ref}
              label={field.label[locale] || ""}
              placeholder={
                (field.placeholder && field.placeholder[locale]) || ""
              }
              rows={field.style?.rows ?? 4}
              value={fieldProps.value as string}
              onChange={(value) => {
                console.log(value);
                setValue(uniqueFieldname, value, setValueConfig);
              }}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
            />
          )}
        </ConnectedField>
      );
    case "NUMBER":
      return (
        <ConnectedField
          formType={formType}
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
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
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
          formType={formType}
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
                  setValue(uniqueFieldname, value, setValueConfig)
                }
              />
            );
          }}
        </ConnectedField>
      );
    case "RESULT_GROUP":
      return (
        <ConnectedField
          formType={formType}
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
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
            />
          )}
        </ConnectedField>
      );
    case "CHECKBOX":
      return (
        <ConnectedField
          formType={formType}
          formField={field}
          control={control}
          defaultValue={defaultValue}
          noLabel
        >
          {({ ref, ...fieldProps }) => (
            <InputCheckbox
              value={fieldProps.value as boolean}
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
              label={field.label[locale] || ""}
              infoTooltip={field.tooltip ? field.tooltip[locale] : undefined}
            />
          )}
        </ConnectedField>
      );
    case "SELECT":
      const options = field.options.map((option) => ({
        label: option.label[locale] || "",
        value: option.value,
      }));

      return (
        <ConnectedField
          formType={formType}
          formField={field}
          control={control}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <InputSelect
              label={field.label[locale]}
              options={options}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              optional={optional}
              value={fieldProps.value as SelectOptionT | null}
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
            />
          )}
        </ConnectedField>
      );
    case "DATASET_SELECT":
      const datasetDefaultValue =
        availableDatasets.length > 0
          ? availableDatasets.find((opt) => opt.value === datasetId) ||
            availableDatasets[0]
          : null;

      return (
        <ConnectedField
          formType={formType}
          formField={field}
          control={control}
          defaultValue={datasetDefaultValue}
        >
          {({ ref, ...fieldProps }) => {
            return (
              <InputSelect
                label={field.label[locale]}
                options={availableDatasets}
                tooltip={field.tooltip ? field.tooltip[locale] : undefined}
                optional={optional}
                value={fieldProps.value as SelectOptionT | null}
                onChange={(value) =>
                  setValue(uniqueFieldname, value, setValueConfig)
                }
              />
            );
          }}
        </ConnectedField>
      );
    case "GROUP":
      return (
        <>
          {field.label && <Headline>{field.label[locale]}</Headline>}
          {field.description && (
            <Description>{field.description[locale]}</Description>
          )}
          <Group
            style={{
              display: (field.style && field.style.display) || "flex",
              gap: field.style?.display === "grid" ? "0 12px" : "0 8px",
              gridTemplateColumns: `repeat(${
                field.style?.gridColumns || 1
              }, 1fr)`,
            }}
          >
            {field.fields.map((f, i) => {
              const key = getFieldKey(formType, f, i);
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
          </Group>
        </>
      );
    case "TABS":
      return (
        <ConnectedField
          formType={formType}
          control={control}
          formField={field}
          defaultValue={defaultValue}
          noContainer
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
                    setValue(uniqueFieldname, tab, setValueConfig)
                  }
                  options={field.tabs.map((tab) => ({
                    label: () => tab.title[locale] || "",
                    value: tab.name,
                    tooltip: tab.tooltip ? tab.tooltip[locale] : undefined,
                  }))}
                />
                {tabToShow && tabToShow.fields.length > 0 ? (
                  <NestedFields>
                    {tabToShow.fields.map((f, i) => {
                      const key = getFieldKey(formType, f, i);
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
                ) : (
                  <Spacer />
                )}
              </>
            );
          }}
        </ConnectedField>
      );
    case "CONCEPT_LIST":
      return (
        <ConnectedField
          formType={formType}
          control={control}
          formField={field}
          defaultValue={defaultValue}
        >
          {({ ref, ...fieldProps }) => (
            <FormConceptGroup
              fieldName={uniqueFieldname}
              value={fieldProps.value as FormConceptGroupT[]}
              onChange={(value) =>
                setValue(uniqueFieldname, value, setValueConfig)
              }
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
              isValidConcept={(item) =>
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
                        value={
                          /* Because we're essentially adding an extra dynamic field to FormConceptGroupT
                            with the key `field.rowPrefixField.name` */
                          (row as unknown as Record<string, string>)[
                            field.rowPrefixField!.name
                          ]
                        }
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
