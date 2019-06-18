// flow

import type { SelectorType } from "../api/types";

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

export function sortSelects(selects: SelectorType[]) {
  return selects
    .concat() // To avoid mutating the original array
    .sort((a, b) => (a.label < b.label ? -1 : 1));
}
