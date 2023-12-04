import styled from "@emotion/styled";
import { FC, memo, useCallback } from "react";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import type {
  ConceptQueryNodeType,
  FilterWithValueType,
} from "../standard-query-editor/types";
import type { ModeT } from "../ui-components/InputRange";

import ContentCell from "./ContentCell";
import DateColumnSelect from "./DateColumnSelect";
import TableFilters from "./TableFilters";
import TableSelects from "./TableSelects";

const Column = styled("div")`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`;

const MaximizedCell = styled(ContentCell)`
  flex-grow: 1;
`;

interface PropsT {
  node: ConceptQueryNodeType;
  tableIdx: number;
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];

  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string) => void;
  onSetFilterValue: (
    tableIdx: number,
    filterIdx: number,
    value: FilterWithValueType["value"],
  ) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
}

const TableView: FC<PropsT> = ({
  node,
  tableIdx,
  allowlistedSelects,
  blocklistedSelects,

  onSelectTableSelects,
  onSetDateColumn,

  onSetFilterValue,
  onSwitchFilterMode,
  onLoadFilterSuggestions,
}) => {
  const { t } = useTranslation();

  const table = node.tables[tableIdx];

  const displaySelects = !!table.selects && table.selects.length > 0;
  const displayDateColumnOptions =
    !!table.dateColumn && table.dateColumn.options.length > 0;
  const displayFilters = !!table.filters && table.filters.length > 0;

  const setFilterValue = useCallback(
    (filterIdx: number, value: FilterWithValueType["value"]) =>
      onSetFilterValue(tableIdx, filterIdx, value),
    [tableIdx, onSetFilterValue],
  );

  const setFilterMode = useCallback(
    (filterIdx: number, mode: ModeT) =>
      onSwitchFilterMode(tableIdx, filterIdx, mode),
    [tableIdx, onSwitchFilterMode],
  );

  const loadFilterSuggestions = useCallback(
    (
      filterIdx: number,
      filterId: string,
      prefix: string,
      page: number,
      pageSize: number,
      config?: { returnOnly?: boolean },
    ) =>
      onLoadFilterSuggestions(
        {
          filterId,
          prefix,
          page,
          pageSize,
        },
        tableIdx,
        filterIdx,
        config,
      ),

    [onLoadFilterSuggestions, tableIdx],
  );

  const selectTableSelects = useCallback(
    (value: SelectOptionT[]) => onSelectTableSelects(tableIdx, value),
    [onSelectTableSelects, tableIdx],
  );

  return (
    <Column>
      {displaySelects && (
        <ContentCell headline={t("queryNodeEditor.selects")}>
          {table.selects && table.selects.length > 0 && (
            <TableSelects
              selects={table.selects}
              allowlistedSelects={allowlistedSelects}
              blocklistedSelects={blocklistedSelects}
              onSelectTableSelects={selectTableSelects}
              excludeTable={table.exclude}
            />
          )}
        </ContentCell>
      )}
      {displayDateColumnOptions && (
        <ContentCell headline={t("queryNodeEditor.selectValidityDate")}>
          <DateColumnSelect
            dateColumn={table.dateColumn!}
            onSelectDateColumn={(value) => onSetDateColumn(tableIdx, value)}
          />
        </ContentCell>
      )}
      {displayFilters && (
        <MaximizedCell headline={t("queryNodeEditor.filters")}>
          <TableFilters
            key={tableIdx}
            filters={table.filters}
            excludeTable={table.exclude}
            onSetFilterValue={setFilterValue}
            onSwitchFilterMode={setFilterMode}
            onLoadFilterSuggestions={loadFilterSuggestions}
          />
        </MaximizedCell>
      )}
    </Column>
  );
};

export default memo(TableView);
