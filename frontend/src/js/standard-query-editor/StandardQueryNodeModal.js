import type { Dispatch }    from 'redux-thunk';
import { connect }          from 'react-redux';

import { QueryNodeModal }   from '../query-node-modal';

import {
  clearStandardNode,
  toggleStandardTable,
  setStandardFilterValue,
  switchStandardFilterMode,
  resetStandardAllFilters,
  toggleStandardTimestamps,
  loadStandardFilterSuggestions,
}                           from './actions';

function findNode(query, andIdx, orIdx) {
  if (!query[andIdx]) return null;

  return query[andIdx].elements[orIdx];
}

function mapStateToProps(state) {
  const node = findNode(
    state.query,
    state.queryNodeModal.andIdx,
    state.queryNodeModal.orIdx
  );

  const showTables = node && node.tables && (
    node.tables.length > 1 ||
    node.tables.some(table => table.filters && table.filters.length > 0)
  );

  return {
    node,
    showTables,
    isExcludeTimestampsPossible: true,
    andIdx: state.queryNodeModal.andIdx,
    orIdx: state.queryNodeModal.orIdx,
    datasetId: state.datasets.selectedDatasetId,
  };
}

function mapDispatchToProps(dispatch: Dispatch) {
  return {
    onCloseModal: () => dispatch(clearStandardNode()),
    onToggleTable: (andIdx, orIdx, tableIdx, isExcluded) =>
      dispatch(toggleStandardTable(
        andIdx,
        orIdx,
        tableIdx,
        isExcluded
      )),
    onSetFilterValue: (andIdx, orIdx, tableIdx, filterIdx, value) =>
      dispatch(setStandardFilterValue(
        andIdx,
        orIdx,
        tableIdx,
        filterIdx,
        value
      )),
    onSwitchFilterMode: (andIdx, orIdx, tableIdx, filterIdx, mode) =>
      dispatch(switchStandardFilterMode(
        andIdx,
        orIdx,
        tableIdx,
        filterIdx,
        mode
      )),
    onResetAllFilters: (andIdx, orIdx) =>
      dispatch(resetStandardAllFilters(andIdx, orIdx)),
    onToggleTimestamps: (andIdx, orIdx, isExcluded) =>
      dispatch(toggleStandardTimestamps(andIdx, orIdx, isExcluded)),
    onLoadFilterSuggestions:
      (datasetId, andIdx, orIdx, tableIdx, tableId, conceptId, filterIdx, filterId, prefix) =>
        dispatch(loadStandardFilterSuggestions(
          datasetId,
          andIdx,
          orIdx,
          tableIdx,
          tableId,
          conceptId,
          filterIdx,
          filterId,
          prefix
      )),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(QueryNodeModal);
