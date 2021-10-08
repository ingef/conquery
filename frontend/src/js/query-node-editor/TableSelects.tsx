import React from "react";

import type { FilterSuggestion, SelectOptionT } from "../api/types";
import { sortSelects } from "../model/select";
import { SelectedSelectorT } from "../standard-query-editor/types";
import InputMultiSelect from "../ui-components/InputMultiSelect";

interface PropsT {
  selects: SelectedSelectorT[];
  onSelectTableSelects: (
    value: SelectOptionT[] | FilterSuggestion[] | null,
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
