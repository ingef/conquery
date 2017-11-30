// @flow

export const selectFormState = (state: Object, formType: string) => state.form[formType];
export const selectReduxFormState = (state: Object) => state.form.reduxForm;
