// @flow

import React    from 'react';

import { Pane } from '../pane';
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
  StatisticsQueryRunner,
  StatisticsFormNavigation,
  StatisticsFormContainer,
} from '../statistics';


type PropsType = {
  activeTab: string
};

const RightPane = (props: PropsType) => {
  let rightPaneContent;

  switch (props.activeTab) {
    case 'queryEditor':
      rightPaneContent = [
        <QueryClearButton key={0} />,
        <QueryEditor key={1} />,
        <StandardQueryRunner key={2} />,
      ];
      break;
    case 'timebasedQueryEditor':
      rightPaneContent = [
        <TimebasedQueryClearButton key={0} />,
        <TimebasedQueryEditor key={1} />,
        <TimebasedQueryRunner key={2} />,
      ]
      break;
    case 'statistics':
      rightPaneContent = [
        <StatisticsFormNavigation key={0} />,
        <StatisticsFormContainer key={1} />,
        <StatisticsQueryRunner key={2} />,
      ]
      break;
    default:
      break;
  }

  return (
    <Pane type="right">
      { rightPaneContent }
    </Pane>
  )
};

export default RightPane;
