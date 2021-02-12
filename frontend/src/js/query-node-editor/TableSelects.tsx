import React from "react";

import type { SelectedSelectorType } from "./types";

import { sortSelects } from "../model/select";

import InputMultiSelect from "../form-components/InputMultiSelect";

interface PropsT {
  selects: SelectedSelectorType[];
  onSelectTableSelects: () => void;
  excludeTable: boolean;
}

const TableSelects = ({
  selects,
  onSelectTableSelects,
  excludeTable,
}: PropsT) => {
  if (!selects || selects.length === 0) return null;

  return (
    <div>
      <InputMultiSelect
        input={{
          onChange: onSelectTableSelects,
          value: selects
            .filter(({ selected }) => !!selected)
            .map(({ id, label }) => ({ value: id, label: label })),
        }}
        options={sortSelects(selects).map((select) => ({
          value: select.id,
          label: select.label,
        }))}
        disabled={excludeTable}
      />
    </div>
  );
};

export default TableSelects;
