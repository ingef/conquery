import { memo, useMemo } from "react";

import type {
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
  FilterT,
  PostFilterSuggestionsResponseT,
  TableT,
} from "../api/types";
import { FilterWithValueType } from "../standard-query-editor/types";
import InputRange, { ModeT } from "../ui-components/InputRange";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import FilterListMultiSelect from "./FilterListMultiSelect";

export interface FiltersContextT {
  datasetId: DatasetT["id"];
  treeId: ConceptIdT;
  tableId: TableT["id"];
}

export interface BaseTableFilterProps {
  className?: string;
  context: FiltersContextT;
  excludeTable?: boolean;
  currencyConfig: CurrencyConfigT;
  onSwitchFilterMode: (filterIdx: number, mode: ModeT) => void;
  onSetFilterValue: (filterIdx: number, value: unknown) => void;
  onLoadFilterSuggestions: (
    tableIdx: number,
    filterId: FilterT["id"],
    prefix: string,
    page: number,
    pageSize: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
}

interface TableFilterProps extends BaseTableFilterProps {
  filter: FilterWithValueType;
  filterIdx: number;
}

const TableFilter = ({
  filter,
  filterIdx,
  excludeTable,
  context,
  className,
  currencyConfig,
  onLoadFilterSuggestions,
  onSetFilterValue,
  onSwitchFilterMode,
}: TableFilterProps) => {
  const filterContext = useMemo(
    () => ({ ...context, filterId: filter.id }),
    [context, filter.id],
  );

  const filterComponent = (() => {
    switch (filter.type) {
      case "SELECT":
        return (
          <InputSelect
            indexPrefix={filterIdx + 1}
            value={
              filter.options.find((o) => o.value === filter.value) ||
              filter.options.find((o) => o.value === filter.defaultValue) ||
              null
            }
            onChange={(value) =>
              onSetFilterValue(filterIdx, value?.value || null)
            }
            label={filter.label}
            tooltip={filter.tooltip}
            options={filter.options}
            disabled={excludeTable}
          />
        );
      case "MULTI_SELECT":
        return (
          <FilterListMultiSelect
            context={filterContext}
            indexPrefix={filterIdx + 1}
            value={filter.value || []}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            label={filter.label}
            tooltip={filter.tooltip}
            options={filter.options}
            disabled={excludeTable}
            allowDropFile={!!filter.allowDropFile}
          />
        );
      case "BIG_MULTI_SELECT":
        return (
          <FilterListMultiSelect
            indexPrefix={filterIdx + 1}
            context={filterContext}
            value={filter.value || []}
            defaultValue={filter.defaultValue}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            label={filter.label}
            tooltip={filter.tooltip}
            options={filter.options}
            disabled={!!excludeTable}
            creatable={!!filter.creatable}
            allowDropFile={!!filter.allowDropFile}
            total={filter.total}
            onLoad={(prefix, page, pageSize, config) =>
              onLoadFilterSuggestions(
                filterIdx,
                filter.id,
                prefix,
                page,
                pageSize,
                config,
              )
            }
          />
        );
      case "INTEGER_RANGE":
        return (
          <InputRange
            indexPrefix={filterIdx + 1}
            value={filter.value}
            defaultValue={filter.defaultValue}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            limits={{ min: filter.min, max: filter.max }}
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            disabled={!!excludeTable}
            onSwitchMode={(mode) => onSwitchFilterMode(filterIdx, mode)}
            placeholder="-"
            pattern={filter.pattern}
          />
        );
      case "REAL_RANGE":
        return (
          <InputRange
            indexPrefix={filterIdx + 1}
            value={filter.value}
            defaultValue={filter.defaultValue}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            limits={{ min: filter.min, max: filter.max }}
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            stepSize={filter.precision || 0.1}
            disabled={!!excludeTable}
            onSwitchMode={(mode) => onSwitchFilterMode(filterIdx, mode)}
            placeholder="-"
            pattern={filter.pattern}
          />
        );
      case "MONEY_RANGE":
        return (
          <InputRange
            indexPrefix={filterIdx + 1}
            moneyRange
            value={filter.value}
            defaultValue={filter.defaultValue}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            disabled={!!excludeTable}
            onSwitchMode={(mode) => onSwitchFilterMode(filterIdx, mode)}
            placeholder="-"
            currencyConfig={currencyConfig}
          />
        );
      default:
        // In the future, there might be other filter types supported
        return null;
    }
  })();

  return filterComponent ? (
    <div className={className}>{filterComponent}</div>
  ) : null;
};

export default memo(TableFilter);
