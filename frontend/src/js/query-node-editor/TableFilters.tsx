import styled from "@emotion/styled";

import type { FilterWithValueType } from "../standard-query-editor/types";

import TableFilter, { BaseTableFilterProps } from "./TableFilter";

interface PropsT extends BaseTableFilterProps {
  filters: FilterWithValueType[] | null;
}

const SxTableFilter = styled(TableFilter)`
  margin-bottom: 10px;
`;

const TableFilters = ({ filters, ...rest }: PropsT) => {
  if (!filters || filters.length === 0) return null;

  return (
    <div>
      {filters.map((filter, filterIdx) => (
        <SxTableFilter
          key={`${rest.context.tableId}-${filter.id}`}
          filter={filter}
          filterIdx={filterIdx}
          {...rest}
        />
      ))}
    </div>
  );
};

export default TableFilters;
