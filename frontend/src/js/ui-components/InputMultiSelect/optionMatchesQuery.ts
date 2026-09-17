import type { SelectOptionT } from "../../api/types";

export const optionMatchesQuery = (option: SelectOptionT, query?: string) => {
  if (!query || option.alwaysShown) return true;

  const lowerQuery = query.toLowerCase();
  const lowerLabel = option.label.toLowerCase();

  return (
    lowerLabel.includes(lowerQuery) ||
    String(option.value).toLowerCase().includes(lowerQuery)
  );
};
