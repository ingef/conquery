// @flow

// The query state contains the form values.
// But small additions are made (properties whitelisted), empty things filtered out
// to make it compatible with the backend API
export const transformFormQueryToApi = (
  query: Object,
  queryType: string,
  formQueryTransformation: Function
): Object => {
  const { form, formName } = query;

  return {
    type: formName,
    ...formQueryTransformation(form, formName)
  }
};
