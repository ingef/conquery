import { combineReducers } from "redux";
import {
  reducer as reduxFormReducer,
  FormReducer,
  FormReducerMapObject,
} from "redux-form";
import createQueryRunnerReducer, {
  QueryRunnerStateT,
} from "../query-runner/reducer";

import { SET_EXTERNAL_FORM, LOAD_EXTERNAL_FORM_VALUES } from "./actionTypes";

import { createFormQueryNodeEditorReducer } from "./form-query-node-editor";
import {
  createFormSuggestionsReducer,
  FormSuggestionsStateT,
} from "./form-suggestions/reducer";
import { collectAllFields } from "./helper";

import type { Forms, Form } from "./config-types";

function collectConceptListFieldNames(config: Form) {
  const fieldNames = collectAllFields(config.fields)
    .filter((field) => field.type === "CONCEPT_LIST")
    .map((field) => field.name);

  return [...new Set(fieldNames)];
}

export interface FormContextStateT {
  suggestions: FormSuggestionsStateT;
}

function buildFormReducer(form: Form) {
  const conceptListFieldNames = collectConceptListFieldNames(form);

  if (conceptListFieldNames.length === 0) return null;

  return combineReducers(
    conceptListFieldNames.reduce(
      (combined, name) => {
        combined[name] = createFormQueryNodeEditorReducer(form.type, name);

        return combined;
      },
      {
        suggestions: createFormSuggestionsReducer(
          form.type,
          conceptListFieldNames
        ),
      }
    )
  );
}

export interface FormsStateT {
  activeForm: string | null;
  queryRunner: QueryRunnerStateT;
  reduxForm: FormReducer;
  availableForms: {
    [formName: string]: Form;
  };
  formsContext: {
    [formName: string]: null | FormContextStateT;
  };
}

const buildExternalFormsReducer = (availableForms: {
  [formName: string]: Form;
}) => {
  const forms = Object.values(availableForms);

  const formReducers = forms.reduce<{
    [formName: string]: null | FormContextStateT;
  }>((all, form) => {
    const reducer = buildFormReducer(form);

    if (!reducer) return all;

    all[form.type] = reducer;

    return all;
  }, {});

  const defaultFormType = forms.length ? forms[0].type : null;

  const activeFormReducer = (
    state: string | null = defaultFormType,
    action: any
  ): string | null => {
    switch (action.type) {
      case SET_EXTERNAL_FORM:
        return action.payload.form;
      default:
        return state;
    }
  };

  const reduxFormReducerPlugin: FormReducerMapObject = forms.reduce(
    (all, form) => ({
      ...all,
      [form.type]: (state: any, action: any) => {
        switch (action.type) {
          case LOAD_EXTERNAL_FORM_VALUES: {
            if (action.payload.formType !== form.type) return state;

            return {
              ...state,
              values: action.payload.values,
            };
          }
          default:
            return state;
        }
      },
    }),
    {}
  );

  return combineReducers({
    activeForm: activeFormReducer,

    // Redux-Form reducer to keep the state of all forms:
    reduxForm: reduxFormReducer.plugin(reduxFormReducerPlugin),

    // Query Runner reducer that works with external forms
    queryRunner: createQueryRunnerReducer("externalForms"),

    availableForms: (state = availableForms) => state,

    formsContext: combineReducers(formReducers),
  });
};

export default buildExternalFormsReducer;
