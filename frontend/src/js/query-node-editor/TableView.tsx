import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  CurrencyConfigT,
  DatasetIdT,
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
  datasetId: DatasetIdT;
  currencyConfig: CurrencyConfigT;
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];

  onShowDescription: (filterIdx: number) => void;
  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
  onSetDateColumn: (tableIdx: number, dateColumnValue: string | null) => void;
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
  ) => void;
}

const TableView: FC<PropsT> = ({
  node,
  tableIdx,
  onShowDescription,
  datasetId,
  currencyConfig,
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

  return (
    <Column>
      {displaySelects && (
        <ContentCell headline={t("queryNodeEditor.selects")}>
          {table.selects && table.selects.length > 0 && (
            <TableSelects
              selects={table.selects}
              allowlistedSelects={allowlistedSelects}
              blocklistedSelects={blocklistedSelects}
              onSelectTableSelects={(value) =>
                onSelectTableSelects(tableIdx, value)
              }
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
            context={{
              datasetId,
              treeId: node.tree,
              tableId: table.id,
            }}
            onSetFilterValue={(filterIdx: number, value: unknown) =>
              onSetFilterValue(tableIdx, filterIdx, value)
            }
            onSwitchFilterMode={(filterIdx, mode) =>
              onSwitchFilterMode(tableIdx, filterIdx, mode)
            }
            onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
              onLoadFilterSuggestions(
                {
                  datasetId: datasetId,
                  conceptId: node.tree,
                  tableId: table.id,
                  filterId,
                  prefix,
                },
                tableIdx,
                filterIdx,
              )
            }
            onShowDescription={onShowDescription}
            currencyConfig={currencyConfig}
          />
        </MaximizedCell>
      )}
    </Column>
  );
};

export default TableView;
