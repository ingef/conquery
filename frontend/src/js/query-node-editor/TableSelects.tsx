import { useMemo } from "react";

import type { SelectOptionT, SelectorResultType } from "../api/types";
import { isSelectDisabled, isValidSelect, sortSelects } from "../model/select";
import type { SelectedSelectorT } from "../standard-query-editor/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

interface PropsT {
  label: string;
  selects: SelectedSelectorT[];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  onSelectTableSelects: (value: SelectOptionT[]) => void;
  excludeTable?: boolean;
}

const TableSelects = ({
  label,
  selects,
  blocklistedSelects,
  allowlistedSelects,
  onSelectTableSelects,
  excludeTable,
}: PropsT) => {
  const options = useMemo(() => {
    return sortSelects(selects).map((select) => ({
      value: select.id,
      label: select.label,
      disabled: isSelectDisabled(select, {
        blocklistedSelects,
        allowlistedSelects,
      }),
    }));
  }, [selects, allowlistedSelects, blocklistedSelects]);

  const value = useMemo(() => {
    return selects
      .filter(isValidSelect({ blocklistedSelects, allowlistedSelects }))
      .map(({ id, label }) => ({ value: id, label: label }));
  }, [selects, allowlistedSelects, blocklistedSelects]);

  return (
    <div>
      <InputMultiSelect
        label={label}
        onChange={onSelectTableSelects}
        value={value}
        options={options}
        disabled={excludeTable}
      />
    </div>
  );
};

export default TableSelects;
