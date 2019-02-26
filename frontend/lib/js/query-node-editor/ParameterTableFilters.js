// @flow

import React from "react";

import {
  InputSelect,
  InputMultiSelect,
  InputRange,
  AsyncInputMultiSelect,
  InputWithLabel
} from "../form-components";

import {
  SELECT,
  MULTI_SELECT,
  INTEGER_RANGE,
  REAL_RANGE,
  MONEY_RANGE,
  STRING,
  BIG_MULTI_SELECT
} from "../form-components/filterTypes";

import type {
  FilterWithValueType,
  CurrencyType
} from "../standard-query-editor/types";

type PropsType = {
  filters: ?(FilterWithValueType[]),
  className?: string,
  excludeTable: boolean,
  onSwitchFilterMode: Function,
  onSetFilterValue: Function,
  onLoadFilterSuggestions: Function,
  onShowDescription: Function,
  suggestions: ?Object,
  onDropFiles: Function,
  currencyConfig: CurrencyType
};

const ParameterTableFilters = (props: PropsType) => {
  if (!props.filters) return null;

  return (
    <div>
      {props.filters
        .map((filter, filterIdx) => {
          switch (filter.type) {
            case SELECT:
              return (
                <InputSelect
                  input={{
                    clearable: filter.value !== filter.defaultValue,
                    defaultValue: filter.defaultValue,
                    value: filter.value,
                    onChange: value => props.onSetFilterValue(filterIdx, value)
                  }}
                  label={filter.label}
                  options={filter.options}
                  disabled={props.excludeTable}
                />
              );
            case MULTI_SELECT:
              return (
                <InputMultiSelect
                  input={{
                    onChange: value => props.onSetFilterValue(filterIdx, value),
                    defaultValue: filter.defaultValue,
                    value: filter.value
                  }}
                  label={filter.label}
                  options={filter.options}
                  disabled={props.excludeTable}
                  onDropFiles={files =>
                    props.onDropFiles(filterIdx, filter.id, files)
                  }
                  allowDropFile={!!filter.allowDropFile}
                />
              );
            case BIG_MULTI_SELECT:
              return (
                <AsyncInputMultiSelect
                  input={{
                    value: filter.value,
                    defaultValue: filter.defaultValue,
                    onChange: value => props.onSetFilterValue(filterIdx, value)
                  }}
                  label={filter.label}
                  options={
                    filter.options ||
                    (props.suggestions &&
                      props.suggestions[filterIdx] &&
                      props.suggestions[filterIdx].options)
                  }
                  isLoading={
                    filter.isLoading ||
                    (props.suggestions &&
                      props.suggestions[filterIdx] &&
                      props.suggestions[filterIdx].isLoading)
                  }
                  startLoadingThreshold={filter.threshold || 1}
                  onLoad={prefix =>
                    props.onLoadFilterSuggestions(filterIdx, filter.id, prefix)
                  }
                  onDropFiles={files =>
                    props.onDropFiles(filterIdx, filter.id, files)
                  }
                  allowDropFile={!!filter.allowDropFile}
                  disabled={!!props.excludeTable}
                />
              );
            case INTEGER_RANGE:
              return (
                <InputRange
                  inputType="number"
                  input={{
                    value: filter.value,
                    defaultValue: filter.defaultValue,
                    onChange: value => props.onSetFilterValue(filterIdx, value)
                  }}
                  limits={{ min: filter.min, max: filter.max }}
                  unit={filter.unit}
                  label={filter.label}
                  mode={filter.mode || "range"}
                  disabled={!!props.excludeTable}
                  onSwitchMode={mode =>
                    props.onSwitchFilterMode(filterIdx, mode)
                  }
                  placeholder="-"
                  pattern={filter.pattern}
                />
              );
            case REAL_RANGE:
              return (
                <InputRange
                  inputType="number"
                  input={{
                    value: filter.value,
                    defaultValue: filter.defaultValue,
                    onChange: value => props.onSetFilterValue(filterIdx, value)
                  }}
                  limits={{ min: filter.min, max: filter.max }}
                  unit={filter.unit}
                  label={filter.label}
                  mode={filter.mode || "range"}
                  stepSize={filter.precision || 0.1}
                  disabled={!!props.excludeTable}
                  onSwitchMode={mode =>
                    props.onSwitchFilterMode(filterIdx, mode)
                  }
                  placeholder="-"
                  pattern={filter.pattern}
                />
              );
            case MONEY_RANGE:
              return (
                <InputRange
                  inputType="text"
                  valueType={MONEY_RANGE}
                  input={{
                    value: filter.value || "",
                    formattedValue: filter.formattedValue,
                    onChange: (value, formattedValue) =>
                      props.onSetFilterValue(filterIdx, value, formattedValue)
                  }}
                  unit={filter.unit}
                  label={filter.label}
                  mode={filter.mode || "range"}
                  disabled={!!props.excludeTable}
                  onSwitchMode={mode =>
                    props.onSwitchFilterMode(filterIdx, mode)
                  }
                  placeholder="-"
                  currencyConfig={props.currencyConfig}
                />
              );
            case STRING:
              return (
                <InputWithLabel
                  inputType="text"
                  input={{
                    value: filter.value || "",
                    defaultValue: filter.defaultValue,
                    onChange: value => props.onSetFilterValue(filterIdx, value)
                  }}
                  placeholder="-"
                  label={filter.label}
                />
              );
            default:
              // In the future, there might be other filter types supported
              return null;
          }
        })
        .filter(input => !!input)
        .map((input, filterIdx) => (
          <div
            key={filterIdx}
            className="query-node-editor__row"
            onFocusCapture={() => props.onShowDescription(filterIdx)}
          >
            {input}
          </div>
        ))}
    </div>
  );
};

export default ParameterTableFilters;
