// flow

const transformExampleFormQueryToApi = (exampleForm) => {
  // POSSIBLY: Transform example values before sending them somewhere
  return exampleForm;
};

const transformFormQuery = (form, formName) => {
  switch (formName) {
    case 'EXAMPLE_FORM':
      return transformExampleFormQueryToApi(form)
    default:
      return form;
  }
}

// The query state contains the form values.
// But small additions are made (properties whitelisted), empty things filtered out
// to make it compatible with the backend API
export const transformFormQueryToApi = (query: Object, queryType: string, version: any): Object => {
  const { form, formName } = query;

  return {
    type: formName,
    ...transformFormQuery(form, formName)
  }
}
