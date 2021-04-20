// The query state contains the form values.
// But small additions are made (properties allowlisted), empty things filtered out
// to make it compatible with the backend API
export const transformFormQueryToApi = (
  query: { form: string; formName: string },
  formQueryTransformation: Function,
): Object => {
  const { form, formName } = query;

  return {
    type: formName,
    ...formQueryTransformation(form, formName),
  };
};
