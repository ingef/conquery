import { getType } from "typesafe-actions";

import type { Action } from "../app/actions";

import { loadFormsSuccess, setExternalForm } from "./actions";
import type { Form, GeneralField } from "./config-types";
import { getUniqueFieldname } from "./helper";

const transformToUniqueFieldnames = (
  formType: string,
  fields: GeneralField[],
): GeneralField[] => {
  return fields.map((field) => {
    switch (field.type) {
      case "HEADLINE":
      case "DESCRIPTION":
        return field;
      case "GROUP":
        return {
          ...field,
          fields: transformToUniqueFieldnames(formType, field.fields),
        };
      case "TABS":
        return {
          ...field,
          tabs: field.tabs.map((tab) => ({
            ...tab,
            fields: transformToUniqueFieldnames(formType, tab.fields),
          })),
        };
      default:
        return {
          ...field,
          name: getUniqueFieldname(formType, field.name),
        };
    }
  });
};

export const availableFormsReducer = (
  state: {
    [formName: string]: Form;
  } = {},
  action: Action,
) => {
  switch (action.type) {
    case getType(loadFormsSuccess):
      return Object.fromEntries(
        action.payload.forms.map((form) => [
          form.type,
          {
            ...form,
            fields: transformToUniqueFieldnames(form.type, form.fields),
          },
        ]),
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
