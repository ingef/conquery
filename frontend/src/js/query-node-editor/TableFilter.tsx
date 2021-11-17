import { memo } from "react";

import {
  ConceptIdT,
  CurrencyConfigT,
  DatasetIdT,
  FilterIdT,
  TableIdT,
} from "../api/types";
import { FilterWithValueType } from "../standard-query-editor/types";
import InputRange, { ModeT } from "../ui-components/InputRange";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import FilterListMultiSelect from "./FilterListMultiSelect";

export interface FiltersContextT {
  datasetId: DatasetIdT;
  treeId: ConceptIdT;
  tableId: TableIdT;
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
    filterId: FilterIdT,
    prefix: string,
  ) => void;
  onShowDescription: (filterIdx: number) => void;
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
  onShowDescription,
  onSwitchFilterMode,
}: TableFilterProps) => {
  const filterComponent = (() => {
    switch (filter.type) {
      case "SELECT":
        return (
          <InputSelect
            indexPrefix={filterIdx + 1}
            defaultValue={filter.defaultValue}
            value={filter.options.find((o) => o.value === filter.value) || null}
            onChange={(value) =>
              onSetFilterValue(filterIdx, value?.value || null)
            }
            label={filter.label}
            options={filter.options}
            disabled={excludeTable}
          />
        );
      case "MULTI_SELECT":
        const defaultValue = filter.options.find(
          (opt) => opt.value === filter.defaultValue,
        );
        return (
          <FilterListMultiSelect
            context={{ ...context, filterId: filter.id }}
            indexPrefix={filterIdx + 1}
            value={filter.value || []}
            defaultValue={defaultValue}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            label={filter.label}
            options={filter.options}
            disabled={excludeTable}
            allowDropFile={!!filter.allowDropFile}
          />
        );
      case "BIG_MULTI_SELECT":
        return (
          <FilterListMultiSelect
            indexPrefix={filterIdx + 1}
            context={{ ...context, filterId: filter.id }}
            value={filter.value || []}
            defaultValue={filter.defaultValue || []}
            onChange={(value) => onSetFilterValue(filterIdx, value)}
            label={filter.label}
            options={filter.options}
            disabled={!!excludeTable}
            allowDropFile={!!filter.allowDropFile}
            isLoading={filter.isLoading}
            onLoad={(prefix: string) =>
              onLoadFilterSuggestions(filterIdx, filter.id, prefix)
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
    <div
      className={className}
      onFocusCapture={() => onShowDescription(filterIdx)}
    >
      {filterComponent}
    </div>
  ) : null;
};

export default memo(TableFilter);
