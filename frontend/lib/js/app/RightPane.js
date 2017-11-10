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
  FormQueryRunner,
  FormNavigation,
  FormContainer,
} from '../form';


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
    case 'form':
      rightPaneContent = [
        <FormNavigation key={0} />,
        <FormContainer key={1} />,
        <FormQueryRunner key={2} />,
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
