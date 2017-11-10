// @flow

import React      from 'react';
import classnames from 'classnames';

import {
  isEmpty,
  includes,
} from '../common/helpers';

import {
  SUPPORTED_FILTERS,
} from '../form';

import type { TableType } from '../standard-query-editor/types';

import ParameterTableFilters from './ParameterTableFilters';


type PropsType = {
  table: TableType,
  allowToggleTable: boolean,
  suggestions: ?Object,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onSwitchFilterMode: Function,
  onLoadFilterSuggestions: Function,
};

const ParameterTable = (props: PropsType) => {
  const tableClass = "parameter-table";
  const isExcluded = !!props.table.exclude;
  const hasFilterValues = (
    props.table.filters &&
    props.table.filters.some(f => !isEmpty(f.value))
  );

  const hasFilters = props.table.filters &&
                     props.table.filters.some(
                       f => includes(Object.keys(SUPPORTED_FILTERS), f.type)
                     );

  return (
    <div className={classnames(
      tableClass, {
        [`${tableClass}--value-changed`]: isExcluded || hasFilterValues
      }
    )}>
      <div className={`${tableClass}__content`}>
        <button
          type="button"
          className={classnames(
            `${tableClass}__head`, {
              [`${tableClass}__head--disabled`]: isExcluded
            },
            'btn',
            'btn--header-transparent', {
              'btn--header-transparent--with-content': hasFilters
            }
          )}
          onClick={props.onToggleTable}
          disabled={!props.allowToggleTable}
        >
          <i className={classnames(
            `${tableClass}__exclude-icon`,
            'fa', {
              'fa-square-o': isExcluded,
              'fa-check-square-o': !isExcluded
            }
          )} />
          {props.table.label}
        </button>
        <ParameterTableFilters
          filters={props.table.filters}
          className={`${tableClass}__body`}
          excludeTable={isExcluded}
          onSetFilterValue={props.onSetFilterValue}
          onSwitchFilterMode={props.onSwitchFilterMode}
          onLoadFilterSuggestions={props.onLoadFilterSuggestions}
          suggestions={props.suggestions}
        />
      </div>
    </div>
  );
};

export default ParameterTable;
