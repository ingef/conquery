import type { SelectorResultType, SelectorT } from "../api/types";
import type {
  ConceptQueryNodeType,
  SelectedSelectorT,
  TableWithFilterValueT,
} from "../standard-query-editor/types";

import type { NodeResetConfig } from "./node";

export function objectHasNonDefaultSelects(
  obj: ConceptQueryNodeType | TableWithFilterValueT,
) {
  return (
    obj &&
    obj.selects &&
    obj.selects.some(
      (select) =>
        (select.selected && !select.default) ||
        (!select.selected && !!select.default),
    )
  );
}

export function sortSelects(selects: SelectorT[]) {
  return selects
    .concat() // To avoid mutating the original array
    .sort((a, b) => (a.label < b.label ? -1 : 1));
}

const resetSelected = (select: SelectorT, config: NodeResetConfig) => ({
  ...select,
  selected: config.useDefaults ? !!select.default : false,
});

export const resetSelects = (
  selects?: SelectorT[],
  config: NodeResetConfig = {},
): SelectedSelectorT[] =>
  selects ? selects.map((select) => resetSelected(select, config)) : [];

function selectTypesMatch(
  resultType1: SelectorResultType,
  resultType2: SelectorResultType,
) {
  if (
    resultType1.type === "LIST" &&
    resultType2.type === "LIST" &&
    !!resultType1.elementType &&
    !!resultType2.elementType
  ) {
    return resultType1.elementType.type === resultType2.elementType.type;
  }

  return resultType1.type === resultType2.type;
}

export function selectIsWithinTypes(
  select: SelectorT,
  types: SelectorResultType[],
) {
  return types.some((selectType) =>
    selectTypesMatch(selectType, select.resultType),
  );
}

export const isSelectDisabled = (
  select: SelectorT,
  blocklistedSelects?: SelectorResultType[],
  allowlistedSelects?: SelectorResultType[],
) =>
  (!!allowlistedSelects && !selectIsWithinTypes(select, allowlistedSelects)) ||
  (!!blocklistedSelects && selectIsWithinTypes(select, blocklistedSelects));
