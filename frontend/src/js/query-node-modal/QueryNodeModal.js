// @flow

import React                from 'react';
import T                    from 'i18n-react';
import classnames           from 'classnames';

import { Modal }            from '../modal';
import ParameterTable       from './ParameterTable';

type PropsType = {
  name: string,
  node: ?Object,
  showTables: boolean,
  andIdx: number,
  orIdx: number,
  isExcludeTimestampsPossible: boolean,
  onCloseModal: Function,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onResetAllFilters: Function,
  onToggleTimestamps: Function,
  onSwitchFilterMode: Function,
  onLoadFilterSuggestions: Function,
  datasetId: number,
  suggestions: ?Object,
};

const QueryNodeModal = (props: PropsType) => {
  const { node } = props;

  if (!node) return null;

  return (
    <Modal closeModal={props.onCloseModal} doneButton>
      <div className="query-node-modal">
        <h3 className="query-node-modal__headline">{node.label}</h3>
        {
          node.description &&
          <p className="query-node-modal__description">{node.description}</p>
        }
        <p className="query-node-modal__explanation">
          { T.translate('queryNodeModal.explanation') }
          {
            node.hasActiveFilters &&
            <span
              className="query-node-modal__reset-all"
              onClick={() => props.onResetAllFilters(props.andIdx, props.orIdx)}
            >
              <i className="fa fa-undo" /> {T.translate('queryNodeModal.resetAll')}
            </span>
          }
        </p>
        {
          props.isExcludeTimestampsPossible &&
          <button
            type="button"
            className="query-node-modal__toggle-timestamps btn btn--header-transparent"
            onClick={() => props.onToggleTimestamps(
              props.andIdx,
              props.orIdx,
              !node.excludeTimestamps
            )}
          >
            <i className={classnames(
              'parameter-table__exclude-icon',
              'fa',
              {
                'fa-square-o': !node.excludeTimestamps,
                'fa-check-square-o': node.excludeTimestamps
              }
            )} /> {T.translate('queryNodeModal.excludeTimestamps')}
          </button>
        }
        <div className="query-node-modal__tables">
          {
            props.showTables && node.tables.map((table, tableIdx) => (
              <ParameterTable
                table={table}
                key={tableIdx}
                allowToggleTable={node.tables.length > 1}
                onToggleTable={() => props.onToggleTable(
                  props.andIdx,
                  props.orIdx,
                  tableIdx,
                  !table.exclude
                )}
                onSetFilterValue={(filterIdx, value) => props.onSetFilterValue(
                  props.andIdx,
                  props.orIdx,
                  tableIdx,
                  filterIdx,
                  value
                )}
                onSwitchFilterMode={(filterIdx, mode) => props.onSwitchFilterMode(
                  props.andIdx,
                  props.orIdx,
                  tableIdx,
                  filterIdx,
                  mode
                )}
                onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
                  props.onLoadFilterSuggestions(
                    props.datasetId,
                    props.andIdx,
                    props.orIdx,
                    tableIdx,
                    node.tables[tableIdx].id,
                    node.id,
                    filterIdx,
                    filterId,
                    prefix
                )}
                suggestions={props.suggestions && props.suggestions[tableIdx]}
              />
            ))
          }
        </div>
      </div>
    </Modal>
  );
};


export default QueryNodeModal;
