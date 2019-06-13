// @flow

/* ------------------------------ */
/* COMMON, FORMS, TABS */
/* ------------------------------ */
type TranslatableString = {
  de: string,
  en?: string
};

export type Forms = Form[];

type Form = {
  type: string, // Sent to api
  title: TranslatableString, // Displayed
  headline: TranslatableString, // Displayed
  fields: (Field | Tabs)[]
};

type Tabs = {
  name: string,
  type: "TABS",
  tabs: Tab[]
};
type Tab = {
  name: string,
  title: TranslatableString,
  fields: Field[]
};

type Field =
  | CheckboxField
  | StringField
  | NumberField
  | SelectField
  | DatasetSelectField
  | MultiSelectField
  | ResultGroupField
  | ConceptListField
  | DateRangeField;

/* ------------------------------ */
/* VALIDATIONS */
/* ------------------------------ */
type NOT_EMPTY_VALIDATION = "NOT_EMPTY";
type GREATER_THAN_ZERO_VALIDATION = "GREATER_THAN_ZERO";

/* ------------------------------ */
/* FIELDS AND THEIR VALIDATIONS */
/* ------------------------------ */
type CommonField = {
  name: string, // Sent to backend
  label: TranslatableString // Used to display
};

/* ------------------------------ */

type CheckboxField = CommonField & {
  type: "CHECKBOX",
  defaultValue?: boolean // Default: False
};

/* ------------------------------ */

type StringFieldValidation = NOT_EMPTY_VALIDATION;
type StringField = CommonField & {
  type: "STRING",
  placeholder?: TranslatableString,
  defaultValue?: string, // Default: ""
  style?: {
    fullWidth?: boolean // Default: False
  },
  pattern?: string, // Regex to validate, using double backslashes, e.g.: "^(?!-)\\\\d*$"
  validations?: StringFieldValidation[]
};

/* ------------------------------ */

type NumberFieldValidation =
  | NOT_EMPTY_VALIDATION
  | GREATER_THAN_ZERO_VALIDATION;
type NumberField = CommonField & {
  type: "NUMBER",
  defaultValue?: number, // Default: null
  placeholder?: TranslatableString,
  pattern?: string, // Regex to validate, using double backslashes, e.g.: "^(?!-)\\\\d*$"
  step?: string,
  min?: number,
  max?: number,
  validations?: NumberFieldValidation[]
};

/* ------------------------------ */

type SelectValue = string;
type SelectOption = {
  label: TranslatableString,
  value: SelectValue
};
type SelectFieldValidation = NOT_EMPTY_VALIDATION;
type SelectField = CommonField & {
  type: "SELECT",
  options: SelectOption[],
  defaultValue?: SelectValue,
  validations?: SelectFieldValidation[]
};
type DatasetSelectField = CommonField & {
  type: "DATASET_SELECT",
  validations?: SelectFieldValidation[]
};

/* ------------------------------ */

type MultiSelectField = CommonField & {
  type: "MULTI_SELECT",
  options: SelectOption[],
  defaultOption?: SelectValue,
  validations?: SelectFieldValidation[]
};

/* ------------------------------ */

type DateRangeFieldValidation = NOT_EMPTY_VALIDATION;
type DateRangeField = {
  type: "DATE_RANGE",
  validations: DateRangeFieldValidation[]
};

/* ------------------------------ */

type ResultGroupFieldValidation = NOT_EMPTY_VALIDATION;
type ResultGroupField = CommonField & {
  type: "RESULT_GROUP",
  dropzoneLabel: TranslatableString,
  validations?: ResultGroupFieldValidation[]
};

/* ------------------------------ */

type ConceptListFieldValidation = NOT_EMPTY_VALIDATION;
type ConceptListField = CommonField & {
  type: "CONCEPT_LIST",
  conceptDropzoneLabel?: TranslatableString,
  conceptColumnDropzoneLabel?: TranslatableString,
  rowPrefixField?: SelectField, // Used for PSM
  isTwoDimensional?: boolean, // Default: False
  disallowedConceptIds?: string[], // Matching the ID string, lowercased
  validations?: ConceptListFieldValidation[]
};

/* ------------------------------ */
