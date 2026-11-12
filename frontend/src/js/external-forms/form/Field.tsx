import { memo } from "react";
import type {
  Control,
  UseFormRegister,
  UseFormSetValue,
  UseFormTrigger,
} from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useDatasetId } from "../../dataset/selectors";
import type { Language } from "../../localization/useActiveLang";
import type { GeneralField } from "../config-types";
import { Description } from "../form-components/Description";
import { getInitialValue, isFormFieldWithValue } from "../helper";

import type { DynamicFormValues } from "./Form";
import { ControlledCheckboxField } from "./fields/ControlledCheckboxField";
import { ControlledConceptListField } from "./fields/ControlledConceptListField";
import { ControlledDatasetSelectField } from "./fields/ControlledDatasetSelectField";
import { ControlledDateField } from "./fields/ControlledDateField";
import { ControlledDateRangeField } from "./fields/ControlledDateRangeField";
import { ControlledNumberField } from "./fields/ControlledNumberField";
import { ControlledResultGroupField } from "./fields/ControlledResultGroupField";
import { ControlledSelectField } from "./fields/ControlledSelectField";
import { ControlledStringField } from "./fields/ControlledStringField";
import { ControlledTextAreaField } from "./fields/ControlledTextAreaField";
import { DisclosureListField } from "./fields/DisclosureListField";
import { GroupField } from "./fields/GroupField";
import { HeadlineField } from "./fields/HeadlineField";
import { TabsField } from "./fields/TabsField";

const Field = ({
  field,
  ...commonProps
}: {
  formType: string;
  h1Index?: number;
  field: GeneralField;
  locale: Language;
  availableDatasets: SelectOptionT[];
  register: UseFormRegister<DynamicFormValues>;
  setValue: UseFormSetValue<DynamicFormValues>;
  control: Control<DynamicFormValues>;
  trigger: UseFormTrigger<DynamicFormValues>;
}) => {
  const datasetId = useDatasetId();
  const { locale, availableDatasets } = commonProps;

  const defaultValue = isFormFieldWithValue(field)
    ? getInitialValue(field, {
        availableDatasets,
        activeLang: locale,
        datasetId,
      })
    : null;

  switch (field.type) {
    case "HEADLINE":
      return <HeadlineField field={field} commonProps={commonProps} />;
    case "DESCRIPTION":
      return (
        <Description
          // biome-ignore lint/security/noDangerouslySetInnerHtml: labels come from the form configs the backend serves
          dangerouslySetInnerHTML={{ __html: field.label[locale] || "" }}
        />
      );
    case "STRING":
      return (
        <ControlledStringField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "TEXTAREA":
      return (
        <ControlledTextAreaField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "NUMBER":
      return (
        <ControlledNumberField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "DATE":
      return (
        <ControlledDateField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "DATE_RANGE":
      return (
        <ControlledDateRangeField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "RESULT_GROUP":
      return (
        <ControlledResultGroupField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "CHECKBOX":
      return (
        <ControlledCheckboxField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "SELECT":
      return (
        <ControlledSelectField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "DATASET_SELECT":
      return (
        <ControlledDatasetSelectField
          field={field}
          commonProps={commonProps}
          datasetId={datasetId}
        />
      );
    case "DISCLOSURE_LIST":
      return (
        <DisclosureListField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
          datasetId={datasetId}
        />
      );
    case "GROUP":
      return <GroupField field={field} commonProps={commonProps} />;
    case "TABS":
      return (
        <TabsField
          field={field}
          commonProps={commonProps}
          defaultValue={defaultValue}
        />
      );
    case "CONCEPT_LIST":
      return (
        <ControlledConceptListField
          field={field}
          commonProps={commonProps}
          defaultValue={defaultValue}
        />
      );
    default:
      return null;
  }
};

export default memo(Field);
