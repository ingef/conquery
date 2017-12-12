// @flow

export const selectFormState = (state: Object, formType: string) => state.externalForms[formType];
export const selectReduxFormState = (state: Object) => state.externalForms.reduxForm;
