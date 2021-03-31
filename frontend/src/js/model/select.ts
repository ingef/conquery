// flow

import type { SelectorT } from "../api/types";
import type { SelectedSelectorT } from "../standard-query-editor/types";

export function objectHasSelectedSelects(obj) {
  return (
    obj &&
    obj.selects &&
    obj.selects.some(
      (select) =>
        (select.selected && !select.default) ||
        (!select.selected && !!select.default)
    )
  );
}

export function sortSelects(selects: SelectorT[]) {
  return selects
    .concat() // To avoid mutating the original array
    .sort((a, b) => (a.label < b.label ? -1 : 1));
}

const withDefaultSelect = (select: SelectorT) => ({
  ...select,
  selected: !!select.default,
});

export const selectsWithDefaults = (
  selects?: SelectorT[]
): SelectedSelectorT[] => (selects ? selects.map(withDefaultSelect) : []);
