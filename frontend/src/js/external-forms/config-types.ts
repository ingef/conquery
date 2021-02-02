/* ------------------------------ */
/* COMMON, FORMS, TABS */
/* ------------------------------ */
interface TranslatableString {
  de: string;
  en?: string;
}

export type Forms = Form[];

export type FormField = Field | Tabs;
export type NonFormField = Headline | Description;

export type GeneralField = FormField | NonFormField;

export interface Form {
  type: string; // Sent to backend API
  title: TranslatableString; // Displayed
  headline: TranslatableString; // Displayed
  fields: GeneralField[];
}

interface Tabs {
  name: string; // Sent to backend API
  type: "TABS";
  tabs: Tab[];
  defaultValue: string; // corresponds to Tab.name
}

interface Tab {
  name: string; // // Sent to backend API
  title: TranslatableString;
  fields: GeneralField[];
}

/* ------------------------------ */
/* VALIDATIONS */
/* ------------------------------ */
type NOT_EMPTY_VALIDATION = "NOT_EMPTY";
type GREATER_THAN_ZERO_VALIDATION = "GREATER_THAN_ZERO";

/* ------------------------------ */
/* FIELDS AND THEIR VALIDATIONS */
/* ------------------------------ */
type Field =
  | CheckboxField
  | StringField
  | NumberField
  | SelectField
  | DatasetSelectField
  | MultiSelectField
  | ResultGroupField
  | MultiResultGroupField
  | ConceptListField
  | DateRangeField;

interface CommonField {
  name: string; // Sent to backend API
  label: TranslatableString; // Used to display
}

/* ------------------------------ */

interface Headline {
  type: "HEADLINE";
  label: TranslatableString;
}

/* ------------------------------ */

interface Description {
  type: "DESCRIPTION";
  label: TranslatableString;
}

/* ------------------------------ */

type CheckboxField = CommonField & {
  type: "CHECKBOX";
  defaultValue?: boolean; // Default: False
};

/* ------------------------------ */

type StringFieldValidation = NOT_EMPTY_VALIDATION;
type StringField = CommonField & {
  type: "STRING";
  placeholder?: TranslatableString;
  defaultValue?: string; // Default: ""
  style?: {
    fullWidth?: boolean; // Default: False
  };
  pattern?: string; // Regex to validate, using double backslashes, e.g.: "^(?!-)\\\\d*$"
  validations?: StringFieldValidation[];
};

/* ------------------------------ */

type NumberFieldValidation =
  | NOT_EMPTY_VALIDATION
  | GREATER_THAN_ZERO_VALIDATION;
type NumberField = CommonField & {
  type: "NUMBER";
  defaultValue?: number; // Default: null
  placeholder?: TranslatableString;
  pattern?: string; // Regex to validate, using double backslashes, e.g.: "^(?!-)\\\\d*$"
  step?: string;
  min?: number;
  max?: number;
  validations?: NumberFieldValidation[];
};

/* ------------------------------ */

type SelectValue = string;
type SelectOption = {
  label: TranslatableString;
  value: SelectValue;
};
type SelectFieldValidation = NOT_EMPTY_VALIDATION;
type SelectField = CommonField & {
  type: "SELECT";
  options: SelectOption[];
  defaultValue?: SelectValue;
  validations?: SelectFieldValidation[];
};
type DatasetSelectField = CommonField & {
  type: "DATASET_SELECT";
  validations?: SelectFieldValidation[];
};

/* ------------------------------ */

type MultiSelectField = CommonField & {
  type: "MULTI_SELECT";
  options: SelectOption[];
  defaultOption?: SelectValue;
  validations?: SelectFieldValidation[];
};

/* ------------------------------ */

type DateRangeFieldValidation = NOT_EMPTY_VALIDATION;
type DateRangeField = CommonField & {
  type: "DATE_RANGE";
  validations?: DateRangeFieldValidation[];
};

/* ------------------------------ */

type ResultGroupFieldValidation = NOT_EMPTY_VALIDATION;
type ResultGroupField = CommonField & {
  type: "RESULT_GROUP";
  dropzoneLabel: TranslatableString;
  validations?: ResultGroupFieldValidation[];
};

/* ------------------------------ */

type MultiResultGroupField = CommonField & {
  type: "MULTI_RESULT_GROUP";
  dropzoneLabel: TranslatableString;
  validations?: ResultGroupFieldValidation[];
};

/* ------------------------------ */

type SelectName = string;
export interface ConnectorDefault {
  name: string;
  selects: SelectName[];
}
export interface ConceptListDefaults {
  selects?: SelectName[];
  connectors?: ConnectorDefault[];
}
type ConceptListFieldValidation = NOT_EMPTY_VALIDATION;
type ConceptListField = CommonField & {
  type: "CONCEPT_LIST";
  conceptDropzoneLabel?: TranslatableString;
  conceptColumnDropzoneLabel?: TranslatableString;
  rowPrefixField?: SelectField & { apiType: string }; // Used for PSM with "MATCHES"
  isTwoDimensional?: boolean; // Default: False
  isSingle?: boolean; // Default: False
  defaults?: ConceptListDefaults;
  validations?: ConceptListFieldValidation[];
  // EITHER USE BLACKLISTING OR WHITELISTING OR NONE OF THE TWO:
  blacklistedConceptIds?: string[]; // Matching ("contains") the ID string, lowercased
  whitelistedConceptIds?: string[]; // Matching ("contains") the ID string, lowercased
  // EITHER USE BLACKLISTING OR WHITELISTING OR NONE OF THE TWO:
  blacklistedConnectors?: string[]; // Matching ("contains") the name of the connector / table
  whitelistedConnectors?: string[]; // Matching ("contains") the name of the connector / table
};

/* ------------------------------ */
