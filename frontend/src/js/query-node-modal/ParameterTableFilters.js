// @flow

import React from 'react';

import {
  includes,
} from '../common/helpers';

import {
  InputSelect,
  InputMultiSelect,
  InputRange,
  AsyncInputMultiSelect,
  InputWithLabel,
  SUPPORTED_FILTERS,
  SELECT,
  MULTI_SELECT,
  INTEGER_RANGE,
  REAL_RANGE,
  STRING,
  BIG_MULTI_SELECT,
} from '../form';

import {
  type FilterType
} from '../standard-query-editor/types';

type PropsType = {
  filters: ?FilterType[],
  className?: string,
  excludeTable: boolean,
  onSwitchFilterMode: Function,
  onSetFilterValue: Function,
  onLoadFilterSuggestions: Function,
  suggestions: ?Object,
};

const ParameterTableFilters = (props: PropsType) => (
  props.filters
    ? <div className={props.className}>
      {
        props.filters
          .filter(f => includes(Object.keys(SUPPORTED_FILTERS), f.type))
          .map((filter, filterIdx) => {
            switch (filter.type) {
              case SELECT:
                return (
                  <InputSelect
                    input={{
                      value: filter.value,
                      onChange: (value) => props.onSetFilterValue(filterIdx, value),
                    }}
                    label={filter.label}
                    options={filter.options}
                    disabled={props.excludeTable}
                    tooltip={filter.description}
                  />
                );
              case MULTI_SELECT:
                return (
                  <InputMultiSelect
                    input={{
                      onChange: (value) => props.onSetFilterValue(filterIdx, value),
                      value: filter.value
                    }}
                    label={filter.label}
                    options={filter.options}
                    disabled={props.excludeTable}
                    tooltip={filter.description}
                  />
                );
              case BIG_MULTI_SELECT:
                return (
                  <AsyncInputMultiSelect
                    input={{
                      value: filter.value,
                      onChange: (value) => props.onSetFilterValue(filterIdx, value),
                    }}
                    label={filter.label}
                    options={
                      filter.options ||
                      (props.suggestions && props.suggestions[filterIdx].options)
                    }
                    isLoading={
                      filter.isLoading ||
                      (props.suggestions && props.suggestions[filterIdx].isLoading)
                    }
                    startLoadingThreshold={filter.threshold || 1}
                    onLoad={(prefix) => props.onLoadFilterSuggestions(filterIdx, filter.id, prefix)}
                    tooltip={filter.description}
                    disabled={!!props.excludeTable}
                  />
                );
              case INTEGER_RANGE:
                return (
                  <InputRange
                    inputType="number"
                    input={{
                      value: filter.value,
                      onChange: (value) => props.onSetFilterValue(filterIdx, value),
                    }}
                    limits={{ min: filter.min, max: filter.max }}
                    unit={filter.unit}
                    label={filter.label}
                    mode={filter.mode || 'range'}
                    disabled={!!props.excludeTable}
                    onSwitchMode={(mode) => props.onSwitchFilterMode(filterIdx, mode)}
                    placeholder="-"
                    tooltip={filter.description}
                  />
                );
              case REAL_RANGE:
                return (
                  <InputRange
                    inputType="number"
                    input={{
                      value: filter.value,
                      onChange: (value) => props.onSetFilterValue(filterIdx, value),
                    }}
                    limits={{ min: filter.min, max: filter.max }}
                    unit={filter.unit}
                    label={filter.label}
                    mode={filter.mode || 'range'}
                    stepSize={filter.precision || 0.1}
                    disabled={!!props.excludeTable}
                    onSwitchMode={(mode) => props.onSwitchFilterMode(filterIdx, mode)}
                    placeholder="-"
                    tooltip={filter.description}
                  />
                );
              case STRING:
                return (
                  <InputWithLabel
                    inputType="text"
                    input={{
                      value: filter.value || "",
                      onChange: (value) => props.onSetFilterValue(filterIdx, value)
                    }}
                    placeholder="-"
                    label={filter.label}
                    tooltip={filter.description}
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
              className="parameter-table__filter"
            >
              {input}
            </div>
          ))
        }
        </div>
      : null
);

export default ParameterTableFilters;
