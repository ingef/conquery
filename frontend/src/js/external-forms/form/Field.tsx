import { memo } from "react";
import { Control, UseFormRegister, UseFormSetValue } from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useDatasetId } from "../../dataset/selectors";
import type { Language } from "../../localization/useActiveLang";
import type { GeneralField } from "../config-types";
import { Description } from "../form-components/Description";
import { getInitialValue, isFormField } from "../helper";

import type { DynamicFormValues } from "./Form";
import { CheckboxField } from "./fields/CheckboxField";
import { ConceptListField } from "./fields/ConceptListField";
import { DatasetSelectField } from "./fields/DatasetSelectField";
import { DateRangeField } from "./fields/DateRangeField";
import { DisclosureField } from "./fields/DisclosureField";
import { GroupField } from "./fields/GroupField";
import { HeadlineField } from "./fields/HeadlineField";
import { NumberField } from "./fields/NumberField";
import { ResultGroupField } from "./fields/ResultGroupField";
import { SelectField } from "./fields/SelectField";
import { StringField } from "./fields/StringField";
import { TabsField } from "./fields/TabsField";
import { TextAreaField } from "./fields/TextAreaField";

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
}) => {
  const datasetId = useDatasetId();
  const { locale, availableDatasets } = commonProps;

  const defaultValue =
    isFormField(field) && field.type !== "GROUP"
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
          dangerouslySetInnerHTML={{ __html: field.label[locale] || "" }}
        />
      );
    case "STRING":
      return (
        <StringField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "TEXTAREA":
      return (
        <TextAreaField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "NUMBER":
      return (
        <NumberField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "DATE_RANGE":
      return (
        <DateRangeField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "RESULT_GROUP":
      return (
        <ResultGroupField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "CHECKBOX":
      return (
        <CheckboxField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "SELECT":
      return (
        <SelectField
          field={field}
          defaultValue={defaultValue}
          commonProps={commonProps}
        />
      );
    case "DATASET_SELECT":
      return (
        <DatasetSelectField
          field={field}
          commonProps={commonProps}
          datasetId={datasetId}
        />
      );
    case "DISCLOSURE":
      return (
        <DisclosureField
          field={field}
          commonProps={commonProps}
          key={field.name}
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
        <ConceptListField
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
