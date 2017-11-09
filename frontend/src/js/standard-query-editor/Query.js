// @flow

import React                      from 'react';
import { connect }                from 'react-redux';
import type { Dispatch }          from 'redux';
import T                          from 'i18n-react';


import {
  queryGroupModalSetNode,
}                                 from '../query-group-modal/actions';

import {
  loadPreviousQuery,
}                                 from '../previous-queries/list/actions';

import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  expandPreviousQuery,
  setStandardNode,
}                                 from './actions'
import type { StandardQueryType } from './types';

import QueryEditorDropzone        from './QueryEditorDropzone';
import QueryGroup                 from './QueryGroup';


type PropsType = {
  query: StandardQueryType,
  isEmptyQuery: boolean,
  dropAndNode: Function,
  dropOrNode: Function,
  deleteNode: Function,
  deleteGroup: Function,
  toggleExcludeGroup: Function,
  expandPreviousQuery: Function,
  loadPreviousQuery: Function,
  setStandardNode: Function,
  queryGroupModalSetNode: Function,
};

const Query = (props: PropsType) => {
  return (
    <div className="query-editor__query-container">
      {
        props.isEmptyQuery &&
        // Render a large Dropzone
        <QueryEditorDropzone
          isInitial
          onDropNode={props.dropAndNode}
          onLoadPreviousQuery={props.loadPreviousQuery}
        />
      }
      <div className="query-editor__query">
        {
          !props.isEmptyQuery &&
          // Render all query groups plus individual AND / OR dropzones
          props.query.map((group, andIdx) => ([
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={item => props.dropOrNode(item, andIdx)}
                onDeleteNode={orIdx => props.deleteNode(andIdx, orIdx)}
                onDeleteGroup={() => props.deleteGroup(andIdx)}
                onFilterClick={orIdx => props.setStandardNode(andIdx, orIdx)}
                onExpandClick={props.expandPreviousQuery}
                onExcludeClick={() => props.toggleExcludeGroup(andIdx)}
                onDateClick={() => props.queryGroupModalSetNode(andIdx)}
                onLoadPreviousQuery={props.loadPreviousQuery}
              />,
              <p className="query-group-connector">{T.translate('common.and')}</p>
            ])).concat(
              <div
                className="dropzone-wrap"
                key={props.query.length + 1}
              >
                <QueryEditorDropzone
                  isAnd
                  onDropNode={props.dropAndNode}
                  onLoadPreviousQuery={props.loadPreviousQuery}
                />
              </div>
            )
        }
      </div>
    </div>
  );
};

function mapStateToProps(state) {
  return {
    query: state.query,
    isEmptyQuery: state.query.length === 0,

    // only used by other actions
    selectedDatasetId: state.datasets.selectedDatasetId,
    rootConcepts: state.categoryTrees.trees,
  };
}

function mapDispatchToProps(dispatch: Dispatch<*>) {
  return {
    dropAndNode: (item) => dispatch(dropAndNode(item)),
    dropOrNode: (item, andIdx) => dispatch(dropOrNode(item, andIdx)),
    deleteNode: (andIdx, orIdx) => dispatch(deleteNode(andIdx, orIdx)),
    deleteGroup: (andIdx) => dispatch(deleteGroup(andIdx)),
    toggleExcludeGroup: (andIdx) => dispatch(toggleExcludeGroup(andIdx)),
    setStandardNode: (andIdx, orIdx) =>
      dispatch(setStandardNode(andIdx, orIdx)),
    queryGroupModalSetNode: (andIdx) =>
      dispatch(queryGroupModalSetNode(andIdx)),
    expandPreviousQuery: (datasetId, rootConcepts, groups) => {
      dispatch(expandPreviousQuery(rootConcepts, groups));

      groups.forEach(group => {
        group.elements.forEach(element => {
          if (element.type === 'QUERY')
            dispatch(loadPreviousQuery(datasetId, element.id))
        });
      });
    },
    loadPreviousQuery: (datasetId, queryId) =>
      dispatch(loadPreviousQuery(datasetId, queryId)),
  };
}

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  loadPreviousQuery: (queryId) =>
    dispatchProps.loadPreviousQuery(
      stateProps.selectedDatasetId,
      queryId
    ),
  expandPreviousQuery: (groups) =>
    dispatchProps.expandPreviousQuery(
      stateProps.selectedDatasetId,
      stateProps.rootConcepts,
      groups
    ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(Query);
