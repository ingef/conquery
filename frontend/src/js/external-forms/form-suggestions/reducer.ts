import { toUpperCaseUnderscore } from "js/common/helpers";

const updateFormFilterProperty = (
  state: any,
  action: any,
  property: any
): any => {
  const { andIdx, orIdx, filterIdx, tableIdx, fieldName } = action.payload;

  const fieldContent = state[fieldName];
  const andContent = fieldContent && fieldContent[andIdx];
  const orContent = andContent && andContent[orIdx];
  const tableContent = orContent && orContent[tableIdx];
  const filterContent = tableContent && tableContent[filterIdx];

  return {
    ...state,
    [fieldName]: {
      ...fieldContent,
      [andIdx]: {
        ...andContent,
        [orIdx]: {
          ...orContent,
          [tableIdx]: {
            ...tableContent,
            [filterIdx]: {
              ...filterContent,
              ...property,
            },
          },
        },
      },
    },
  };
};

const loadFormFilterSuggestionsStart = (state: any, action: any): any => {
  return updateFormFilterProperty(state, action, { isLoading: true });
};

const loadFormFilterSuggestionsSuccess = (state: any, action: any): any => {
  const { andIdx, orIdx, filterIdx, tableIdx, fieldName } = action.payload;
  const previousOptions =
    (state[fieldName] &&
      state[fieldName][andIdx] &&
      state[fieldName][andIdx][orIdx] &&
      state[fieldName][andIdx][orIdx][tableIdx] &&
      state[fieldName][andIdx][orIdx][tableIdx][filterIdx] &&
      state[fieldName][andIdx][orIdx][tableIdx][filterIdx].options) ||
    [];

  return updateFormFilterProperty(state, action, {
    isLoading: false,
    options: action.payload.suggestions
      // Combine with previous suggestions
      .concat(previousOptions)
      // Remove duplicate items
      .reduce(
        (options, currentOption) =>
          options.find((x) => x.value === currentOption.value)
            ? options
            : [...options, currentOption],
        []
      ),
  });
};

const loadFormFilterSuggestionsError = (state: any, action: any): any => {
  return updateFormFilterProperty(state, action, { isLoading: false });
};

// TODO: SPEC THIS OUT!
export type FormSuggestionsStateT = any;

export const createFormSuggestionsReducer = (
  formType: string,
  fieldNames: string[]
) => {
  const reducerHandlers = fieldNames
    .map((fieldName) => {
      const uppercasedFieldName = toUpperCaseUnderscore(fieldName);

      return {
        [`form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_START`]: loadFormFilterSuggestionsStart,
        [`form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_SUCCESS`]: loadFormFilterSuggestionsSuccess,
        [`form-suggestions/LOAD_${formType}_${uppercasedFieldName}_FILTER_SUGGESTIONS_ERROR`]: loadFormFilterSuggestionsError,
      };
    })
    .reduce((acc, handlers) => ({ ...acc, ...handlers }), {});

  return (
    state: FormSuggestionsStateT = {},
    action: any
  ): FormSuggestionsStateT => {
    if (reducerHandlers[action.type])
      return reducerHandlers[action.type](state, action);

    return state;
  };
};
