import styled from "@emotion/styled";
import { memo } from "react";
import { useSelector } from "react-redux";

import type {
  CurrencyConfigT,
  FilterT,
  PostFilterSuggestionsResponseT,
} from "../api/types";
import { StateT } from "../app/reducers";
import { FilterWithValueType } from "../standard-query-editor/types";
import InputRange, { ModeT } from "../ui-components/InputRange";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import FilterListMultiSelect from "./FilterListMultiSelect";

const Container = styled("div")`
  margin-bottom: 10px;
`;

export interface BaseTableFilterProps {
  className?: string;
  excludeTable?: boolean;
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
    <Container className={className}>{filterComponent}</Container>
  ) : null;
};

export default memo(TableFilter);
