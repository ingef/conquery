// @flow

import React                              from 'react';

import { type TabPropsType }              from '../pane';
import { createQueryRunnerReducer }       from '../query-runner';

import { default as queryReducer }        from './reducer';

import { QueryEditor }                    from './QueryEditor';
import { StandardQueryRunner }            from './StandardQueryRunner';
import { QueryClearButton }               from './QueryClearButton';

const StandardQueryEditor = (props: TabPropsType) =>
  <React.Fragment>
    <QueryClearButton />
    <QueryEditor selectedDatasetId={props.selectedDatasetId} />
    <StandardQueryRunner datasetId={props.selectedDatasetId} />
  </React.Fragment>;

const queryRunnerReducer = createQueryRunnerReducer('standard');

export const StandardQueryEditorTab = {
  key: 'queryEditor',
  label: 'rightPane.queryEditor',
  order: 0,
  reducer: (state = StandardQueryEditorTab, action) => ({
    ...state,
    query: queryReducer(state.query, action),
    queryRunner: queryRunnerReducer(state.queryRunner, action)
  }),
  component: StandardQueryEditor
};
