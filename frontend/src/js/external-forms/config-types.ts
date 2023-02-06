import { SelectorResultType } from "../api/types";

/* ------------------------------ */

/* COMMON, FORMS, TABS */

/* ------------------------------ */
interface TranslatableString {
  de: string;
  en?: string;
}

export type Forms = Form[];

export type FormField = Field | Tabs | Group;
export type NonFormField = Headline | Description;

export type GeneralField = FormField | NonFormField;

export interface Form {
  type: string; // Sent to backend API
  title: TranslatableString; // Displayed
  fields: GeneralField[];
  description?: TranslatableString; // Displayed
  manualUrl?: string;
}

export interface Group {
  type: "GROUP";
  label?: TranslatableString;
  description?: TranslatableString;
  style?: {
    display: "flex" | "grid";
    gridColumns?: number;
  };
  fields: GeneralField[];
}

export interface Tabs {
  name: string; // Sent to backend API
  type: "TABS";
  tabs: Tab[];
  defaultValue: string; // corresponds to Tab.name
}

interface Tab {
  name: string; // // Sent to backend API
  title: TranslatableString;
  tooltip?: TranslatableString;
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
export type Field =
  | CheckboxField
  | StringField
  | TextareaField
  | NumberField
  | SelectField
  | DatasetSelectField
  | ResultGroupField
  | ConceptListField
  | DateRangeField;
// TODO: At some point, handle multi select as well
// | MultiSelectField;

interface CommonField {
  name: string; // Sent to backend API
  label: TranslatableString; // Used to display
  tooltip?: TranslatableString;
}

/* ------------------------------ */

export interface Headline {
  type: "HEADLINE";
  label: TranslatableString;
  style?: {
    size?: "h1" /* default */ | "h2" | "h3";
  };
}

/* ------------------------------ */

interface Description {
  type: "DESCRIPTION";
  label: TranslatableString;
}

/* ------------------------------ */

export type CheckboxField = CommonField & {
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

type TextareaFieldValidation = NOT_EMPTY_VALIDATION;
type TextareaField = CommonField & {
  type: "TEXTAREA";
  placeholder?: TranslatableString;
  defaultValue?: string; // Default: ""
  style?: {
    rows?: number; // Default: 10
  };
  validations?: TextareaFieldValidation[];
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

// TODO: At some point, handle multi select as well
// type MultiSelectField = CommonField & {
//   type: "MULTI_SELECT";
//   options: SelectOption[];
//   defaultValue?: SelectValue[];
//   validations?: SelectFieldValidation[];
// };

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
export type ConceptListField = CommonField & {
  type: "CONCEPT_LIST";
  conceptDropzoneLabel?: TranslatableString;
  conceptColumnDropzoneLabel?: TranslatableString;
  rowPrefixField?: SelectField;
  isTwoDimensional?: boolean; // Default: False
  isSingle?: boolean; // Default: False
  defaults?: ConceptListDefaults;
  validations?: ConceptListFieldValidation[];
  // EITHER USE BLOCKLISTING OR ALLOWLISTING OR NONE OF THE TWO:
  blocklistedConceptIds?: string[]; // Matching ("contains") the ID string, lowercased
  allowlistedConceptIds?: string[]; // Matching ("contains") the ID string, lowercased
  // EITHER USE BLOCKLISTING OR ALLOWLISTING OR NONE OF THE TWO:
  blocklistedConnectors?: string[]; // Matching ("contains") the name of the connector / table
  allowlistedConnectors?: string[]; // Matching ("contains") the name of the connector / table
  // EITHER USE BLOCKLISTING OR ALLOWLISTING OR NONE OF THE TWO:
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
};

/* ------------------------------ */
