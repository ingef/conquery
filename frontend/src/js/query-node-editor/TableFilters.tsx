import type { FilterWithValueType } from "../standard-query-editor/types";

import TableFilter, { BaseTableFilterProps } from "./TableFilter";

interface PropsT extends BaseTableFilterProps {
  filters: FilterWithValueType[] | null;
}

const TableFilters = ({ filters, ...rest }: PropsT) => {
  if (!filters || filters.length === 0) return null;

  return (
    <div>
      {filters.map((filter, filterIdx) => (
        <TableFilter
          key={filter.id}
          filter={filter}
          filterIdx={filterIdx}
          {...rest}
        />
      ))}
    </div>
  );
};

export default TableFilters;
