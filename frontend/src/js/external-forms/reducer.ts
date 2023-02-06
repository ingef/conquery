import { getType } from "typesafe-actions";

import type { Action } from "../app/actions";

import { loadFormsSuccess, setExternalForm } from "./actions";
import type { Form } from "./config-types";

export const availableFormsReducer = (
  state: {
    [formName: string]: Form;
  } = {},
  action: Action,
) => {
  switch (action.type) {
    case getType(loadFormsSuccess):
      return Object.fromEntries(
        action.payload.forms.map((form) => [form.type, form]),
      );
    default:
      return state;
  }
};

export const activeFormReducer = (
  state: string | null = null,
  action: Action,
): string | null => {
  switch (action.type) {
    case getType(setExternalForm):
      return action.payload.form;
    default:
      return state;
  }
};
