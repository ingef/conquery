import React from "react";

import { sortSelects } from "../model/select";

import InputMultiSelect from "../form-components/InputMultiSelect";
import { SelectedSelectorT } from "../standard-query-editor/types";
import type { FilterSuggestion, SelectOptionT } from "../api/types";

interface PropsT {
  selects: SelectedSelectorT[];
  onSelectTableSelects: (
    value: SelectOptionT[] | FilterSuggestion[] | null
  ) => void;
  excludeTable?: boolean;
}

const TableSelects = ({
  selects,
  onSelectTableSelects,
  excludeTable,
}: PropsT) => {
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
