import styled from "@emotion/styled";
import { FC, memo, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  DatasetT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import type { ConceptQueryNodeType } from "../standard-query-editor/types";
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
  datasetId: DatasetT["id"];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];

  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string) => void;
  onSetFilterValue: (tableIdx: number, filterIdx: number, value: any) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
    { returnOnly }?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
}

const TableView: FC<PropsT> = ({
  node,
  tableIdx,
  datasetId,
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

  const filterContext = useMemo(
    () => ({
      datasetId,
      treeId: node.tree,
      tableId: table.id,
    }),
    [node.tree, table.id, datasetId],
  );

  const setFilterValue = useCallback(
    (filterIdx: number, value: unknown) =>
      onSetFilterValue(tableIdx, filterIdx, value),
    [tableIdx, onSetFilterValue],
  );

  const setFilterMode = useCallback(
    (filterIdx: number, mode: ModeT) =>
      onSwitchFilterMode(tableIdx, filterIdx, mode),
    [tableIdx, onSwitchFilterMode],
  );

  const loadFilterSuggestions = useCallback(
    (filterIdx, filterId, prefix, page, pageSize, config) =>
      onLoadFilterSuggestions(
        {
          datasetId: datasetId,
          conceptId: node.tree,
          tableId: table.id,
          filterId,
          prefix,
          page,
          pageSize,
        },
        tableIdx,
        filterIdx,
        config,
      ),

    [onLoadFilterSuggestions, datasetId, node.tree, table.id, tableIdx],
  );

  const selectTableSelects = useCallback(
    (value) => onSelectTableSelects(tableIdx, value),
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
            context={filterContext}
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
