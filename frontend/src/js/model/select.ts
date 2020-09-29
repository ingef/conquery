// flow

import type { SelectorT } from "../api/types";

export function objectHasSelectedSelects(obj) {
  return (
    obj &&
    obj.selects &&
    obj.selects.some(
      select =>
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

const withDefaultSelect = select => ({
  ...select,
  selected: !!select.default
});

export const selectsWithDefaults = selects =>
  selects ? selects.map(withDefaultSelect) : null;
