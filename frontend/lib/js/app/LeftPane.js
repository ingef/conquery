// @flow

import React                        from 'react';
import { Route }                    from 'react-router';

import { Pane }                     from '../pane';
import {
  CategoryTreeList,
  CategoryTreeSearchBox
} from '../category-trees';
import { DatasetSelector }          from '../dataset';
import { DeletePreviousQueryModal } from '../previous-queries/delete-modal';
import { PreviousQueriesSearchBox } from '../previous-queries/search';
import { PreviousQueriesFilter }    from '../previous-queries/filter';
import { PreviousQueriesContainer } from '../previous-queries/list';
import { UploadQueryResults }       from '../previous-queries/upload';
import { templates }                from '../routes';

type PropsType = {
  activeTab: string
};

const LeftPane = (props: PropsType) => (
  <Route path={templates.toDataset} children={({ match }) => {
    const selectedDatasetId = match && match.params ? match.params.datasetId : null;

    return (
      <Pane type="left">
        <DatasetSelector selectedDatasetId={selectedDatasetId} />
        {
          props.activeTab === 'categoryTrees' &&
            <CategoryTreeSearchBox mode="simple" />
        }
        <CategoryTreeList />
        {
          props.activeTab === 'previousQueries' &&
          [
            <PreviousQueriesFilter key={0} />,
            <PreviousQueriesSearchBox key={1} />,
            <UploadQueryResults datasetId={selectedDatasetId} key={2} />,
            <PreviousQueriesContainer datasetId={selectedDatasetId} key={3} />,
            <DeletePreviousQueryModal datasetId={selectedDatasetId} key={4} />,
          ]
        }
      </Pane>
    )
  }} />
);

export default LeftPane;
