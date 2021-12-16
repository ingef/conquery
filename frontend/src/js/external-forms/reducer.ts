import { combineReducers, Reducer } from "redux";
import { getType } from "typesafe-actions";

import type { Action } from "../app/actions";
import type { QueryNodeEditorStateT } from "../query-node-editor/reducer";
import createQueryRunnerReducer, {
  QueryRunnerStateT,
} from "../query-runner/reducer";

import { setExternalForm } from "./actions";
import type { Form } from "./config-types";
import { createFormQueryNodeEditorReducer } from "./form-query-node-editor/reducer";
import { collectAllFormFields } from "./helper";

function collectConceptListFieldNames(config: Form) {
  const fieldNames = collectAllFormFields(config.fields)
    .filter((field) => field.type === "CONCEPT_LIST")
    .map((field) => field.name);

  return [...new Set(fieldNames)];
}

export interface FormContextStateT {
  [conceptListFieldName: string]: QueryNodeEditorStateT;
}

function buildFormReducer(form: Form) {
  const conceptListFieldNames = collectConceptListFieldNames(form);

  if (conceptListFieldNames.length === 0) return () => null;

  return combineReducers(
    conceptListFieldNames.reduce((combined, name) => {
      combined[name] = createFormQueryNodeEditorReducer(form.type, name);

      return combined;
    }, {}),
  );
}

export interface FormsStateT {
  activeForm: string | null;
  queryRunner: QueryRunnerStateT;
  availableForms: {
    [formName: string]: Form;
  };
  formsContext: {
    [formName: string]: null | FormContextStateT;
  };
}

// Because this function is called multiple times, and the reducers are being replaced
// we have to make sure that all reducers in here can deal with a changing default state,
// meaning: return it when the default state changes changes, without expecting another @@INIT action
const buildExternalFormsReducer = (availableForms: {
  [formName: string]: Form;
}) => {
  const forms = Object.values(availableForms);

  const formReducers = forms.reduce<{
    [formName: string]: Reducer<FormContextStateT>;
  }>((all, form) => {
    const reducer = buildFormReducer(form);

    if (!reducer) return all;

    all[form.type] = reducer;

    return all;
  }, {});

  const defaultFormType = forms.length ? forms[0].type : null;

  const activeFormReducer = (
    state: string | null = defaultFormType,
    action: Action,
  ): string | null => {
    switch (action.type) {
      case getType(setExternalForm):
        return action.payload.form;
      default:
        return state || defaultFormType;
    }
  };

  const availableFormsReducer = () => availableForms;

  return combineReducers({
    activeForm: activeFormReducer,

    // Query Runner reducer that works with external forms
    queryRunner: createQueryRunnerReducer("externalForms"),

    availableForms: availableFormsReducer,

    formsContext:
      Object.keys(formReducers).length > 0
        ? combineReducers(formReducers)
        : (state = {}) => state,
  });
};

export default buildExternalFormsReducer;
