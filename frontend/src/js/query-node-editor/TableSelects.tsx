import React from "react";

import type {
  FilterSuggestion,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { isSelectDisabled, sortSelects } from "../model/select";
import { SelectedSelectorT } from "../standard-query-editor/types";
import InputMultiSelect from "../ui-components/InputMultiSelect";

interface PropsT {
  selects: SelectedSelectorT[];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  onSelectTableSelects: (
    value: SelectOptionT[] | FilterSuggestion[] | null,
  ) => void;
  excludeTable?: boolean;
}

const TableSelects = ({
  selects,
  blocklistedSelects,
  allowlistedSelects,
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
          disabled: isSelectDisabled(
            select,
            blocklistedSelects,
            allowlistedSelects,
          ),
        }))}
        disabled={excludeTable}
      />
    </div>
  );
};

export default TableSelects;
