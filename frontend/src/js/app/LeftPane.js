// @flow

import React                        from 'react';

import { Pane }                     from '../pane';
import { CategoryTreeList }         from '../category-trees';
import { DatasetSelector }          from '../dataset';
import { DeletePreviousQueryModal } from '../previous-queries/delete-modal';
import { PreviousQueriesSearchBox } from '../previous-queries/search';
import { PreviousQueriesFilter }    from '../previous-queries/filter';
import { PreviousQueriesContainer } from '../previous-queries/list';
import { UploadQueryResults }       from '../previous-queries/upload';

type PropsType = {
  activeTab: string
};

const LeftPane = (props: PropsType) => (
  <Pane type="left">
    <DatasetSelector />
    <CategoryTreeList />
    {
      props.activeTab === 'previousQueries' &&
      [
        <PreviousQueriesFilter key={0} />,
        <PreviousQueriesSearchBox key={1} />,
        <UploadQueryResults key={2} />,
        <PreviousQueriesContainer key={3} />,
        <DeletePreviousQueryModal key={4} />,
      ]
    }
  </Pane>
);

export default LeftPane;
