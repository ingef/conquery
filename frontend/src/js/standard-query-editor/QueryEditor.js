import React                  from 'react';
import { Route }              from 'react-router';

import { QueryGroupModal }    from '../query-group-modal';
import UploadConceptListModal from '../upload-concept-list-modal/UploadConceptListModal';

import Query                  from './Query';
import StandardQueryNodeModal from './StandardQueryNodeModal';

class QueryEditor extends React.Component {
  render() {
    return (
      <div className="query-editor">
        <Query selectedDatasetId={this.props.selectedDatasetId} />
        <StandardQueryNodeModal datasetId={this.props.selectedDatasetId} />
        <UploadConceptListModal selectedDatasetId={this.props.selectedDatasetId} />
        <QueryGroupModal />
      </div>
    );
  }
}

export default QueryEditor;
