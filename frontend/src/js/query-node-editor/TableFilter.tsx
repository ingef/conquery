import { memo } from "react";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type {
  CurrencyConfigT,
  FilterT,
  PostFilterSuggestionsResponseT,
  RangeFilterValueT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import type { FilterWithValueType } from "../standard-query-editor/types";
import { ComboBoxField } from "../ui-components/ComboBoxField";
import {
  type ModeT,
  NumberRangeField,
} from "../ui-components/NumberRangeField";

import FilterListMultiSelect from "./FilterListMultiSelect";

const container = tv({ base: "mb-[10px]" });

export interface BaseTableFilterProps {
  className?: string;
  excludeTable?: boolean;
  onSwitchFilterMode: (filterIdx: number, mode: ModeT) => void;
  onSetFilterValue: (
    filterIdx: number,
    value: FilterWithValueType["value"],
  ) => void;
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
  className,
  onLoadFilterSuggestions,
  onSetFilterValue,
  onSwitchFilterMode,
}: TableFilterProps) => {
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  const filterComponent = (() => {
    switch (filter.type) {
      case "SELECT":
        return (
          <ComboBoxField
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
            isDisabled={excludeTable}
          />
        );
      case "MULTI_SELECT":
        return (
          <FilterListMultiSelect
            filterId={filter.id}
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
            filterId={filter.id}
            value={filter.value || []}
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
          <NumberRangeField
            indexPrefix={filterIdx + 1}
            value={filter.value}
            onChange={(value) =>
              onSetFilterValue(filterIdx, value as RangeFilterValueT)
            }
            limits={{ min: filter.min, max: filter.max }}
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            stepSize={1}
            isDisabled={!!excludeTable}
            onSwitchMode={(mode) => onSwitchFilterMode(filterIdx, mode)}
            placeholder="-"
          />
        );
      case "REAL_RANGE":
        return (
          <NumberRangeField
            indexPrefix={filterIdx + 1}
            value={filter.value}
            onChange={(value) =>
              onSetFilterValue(filterIdx, value as RangeFilterValueT)
            }
            limits={{ min: filter.min, max: filter.max }}
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            stepSize={filter.precision || 0.1}
            isDisabled={!!excludeTable}
            onSwitchMode={(mode) => onSwitchFilterMode(filterIdx, mode)}
            placeholder="-"
          />
        );
      case "MONEY_RANGE":
        return (
          <NumberRangeField
            indexPrefix={filterIdx + 1}
            moneyRange
            value={filter.value}
            onChange={(value) =>
              onSetFilterValue(filterIdx, value as RangeFilterValueT)
            }
            unit={filter.unit}
            label={filter.label}
            tooltip={filter.tooltip}
            mode={filter.mode || "range"}
            isDisabled={!!excludeTable}
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
      className={container({ className })}
      data-test-id={`table-filter-${filter.id}`}
    >
      {filterComponent}
    </div>
  ) : null;
};

export default memo(TableFilter);
