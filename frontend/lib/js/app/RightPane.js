// @flow

import React         from 'react';
import { Route }     from 'react-router';

import { Pane }      from '../pane';
import { templates } from '../routes';
import {
  QueryEditor,
  StandardQueryRunner,
  QueryClearButton,
} from '../standard-query-editor';

import {
  TimebasedQueryEditor,
  TimebasedQueryRunner,
  TimebasedQueryClearButton,
} from '../timebased-query-editor'

import {
  ExternalFormsQueryRunner,
  ExternalFormsNavigation,
  ExternalFormsContainer,
} from '../external-forms';


type PropsType = {
  activeTab: string
};

const RightPane = (props: PropsType) => {
  const rightPaneContent = (activeTab, selectedDatasetId) => {
    switch (props.activeTab) {
      case 'queryEditor':
        return [
          <QueryClearButton key={0} />,
          <QueryEditor selectedDatasetId={selectedDatasetId} key={1} />,
          <StandardQueryRunner datasetId={selectedDatasetId} key={2} />,
        ];
      case 'timebasedQueryEditor':
        return [
          <TimebasedQueryClearButton key={0} />,
          <TimebasedQueryEditor key={1} />,
          <TimebasedQueryRunner datasetId={selectedDatasetId} key={2} />,
        ]
      case 'externalForms':
        return [
          <ExternalFormsNavigation key={0} />,
          <ExternalFormsContainer datasetId={selectedDatasetId} key={1} />,
          <ExternalFormsQueryRunner datasetId={selectedDatasetId} key={2} />,
        ]
      default:
        return null;
    }
  };

  return (
    <Route path={templates.toDataset} children={({ match }) => {
      const selectedDatasetId = match && match.params ? match.params.datasetId : null;

      return (
        <Pane type="right">
          { rightPaneContent(props.activeTab, selectedDatasetId) }
        </Pane>
      );
    }} />
  )
};

export default RightPane;
