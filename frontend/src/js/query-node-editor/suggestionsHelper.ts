import Mustache from "mustache";

import type { RawFilterSuggestion, SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";

export const isRawFilterSuggestion = (
  option: SelectOptionT | RawFilterSuggestion,
): option is RawFilterSuggestion => {
  return (
    "optionValue" in option &&
    "templateValues" in option &&
    exists(option.templateValues) &&
    exists(option.optionValue)
  );
};

export const filterSuggestionToSelectOption = (
  option: SelectOptionT | RawFilterSuggestion,
): SelectOptionT => {
  return isRawFilterSuggestion(option)
    ? {
        // To avoid filtering loaded suggestions with
        // the search string on the frontend side again
        alwaysShown: true,

        label: Mustache.render(option.label, option.templateValues),
        value: option.value,
        selectedLabel: Mustache.render(
          option.optionValue,
          option.templateValues,
        ),
        disabled: option.disabled,
      }
    : option;
};
