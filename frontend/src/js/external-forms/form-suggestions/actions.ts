import { ConceptIdT, DatasetIdT, FilterIdT, TableIdT } from "../../api/types";
import { useDispatch } from "react-redux";
import { usePostPrefixForSuggestions } from "../../api/api";
import { toUpperCaseUnderscore } from "js/common/helpers";

const loadFormFilterSuggestionsStart = (
  formName,
  fieldName,
  andIdx,
  orIdx,
  tableIdx,
  conceptId,
  filterIdx
) => {
  const uppercasedFieldName = toUpperCaseUnderscore(fieldName);

  return {
    type: `form-suggestions/LOAD_${formName}_${uppercasedFieldName}_FILTER_SUGGESTIONS_START`,
    payload: {
      fieldName,
      andIdx,
      orIdx,
      tableIdx,
      conceptId,
      filterIdx,
    },
  };
};

const loadFormFilterSuggestionsSuccess = (
  suggestions,
  formName,
  fieldName,
  andIdx,
  orIdx,
  tableIdx,
  filterIdx
) => {
  const uppercasedFieldName = toUpperCaseUnderscore(fieldName);

  return {
    type: `form-suggestions/LOAD_${formName}_${uppercasedFieldName}_FILTER_SUGGESTIONS_SUCCESS`,
    payload: {
      suggestions,
      fieldName,
      andIdx,
      orIdx,
      tableIdx,
      filterIdx,
    },
  };
};

const loadFormFilterSuggestionsError = (
  error,
  formName,
  fieldName,
  andIdx,
  orIdx,
  tableIdx,
  filterIdx
) => {
  const uppercasedFieldName = toUpperCaseUnderscore(fieldName);

  return {
    type: `form-suggestions/LOAD_${formName}_${uppercasedFieldName}_FILTER_SUGGESTIONS_ERROR`,
    payload: {
      ...error,
      fieldName,
      andIdx,
      orIdx,
      tableIdx,
      filterIdx,
    },
  };
};

export const useLoadFormFilterSuggestions = () => {
  const dispatch = useDispatch();
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  return (
    formName: string,
    fieldName: string,
    andIdx: number,
    orIdx: number,
    datasetId: DatasetIdT,
    conceptId: ConceptIdT,
    tableId: TableIdT,
    filterId: FilterIdT,
    prefix: string,
    tableIdx: number,
    filterIdx: number
  ) => {
    dispatch(
      loadFormFilterSuggestionsStart(
        formName,
        fieldName,
        andIdx,
        orIdx,
        tableIdx,
        conceptId,
        filterIdx
      )
    );

    return postPrefixForSuggestions(
      datasetId,
      conceptId,
      tableId,
      filterId,
      prefix
    ).then(
      (r) =>
        dispatch(
          loadFormFilterSuggestionsSuccess(
            r,
            formName,
            fieldName,
            andIdx,
            orIdx,
            tableIdx,
            filterIdx
          )
        ),
      (e) =>
        dispatch(
          loadFormFilterSuggestionsError(
            e,
            formName,
            fieldName,
            andIdx,
            orIdx,
            tableIdx,
            filterIdx
          )
        )
    );
  };
};
