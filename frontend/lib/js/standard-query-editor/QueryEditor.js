import React                   from 'react';

import { QueryGroupModal }     from '../query-group-modal';
import UploadConceptListModal  from '../upload-concept-list-modal/UploadConceptListModal';
import type { DatasetIdType }  from '../dataset/reducer';

import Query                   from './Query';
import StandardQueryNodeEditor from './StandardQueryNodeEditor';

type PropsType = {
  selectedDatasetId: DatasetIdType
}

class QueryEditor extends React.Component {
  props: PropsType;

  render() {
    return (
      <div className="query-editor">
        <Query selectedDatasetId={this.props.selectedDatasetId} />
        <StandardQueryNodeEditor datasetId={this.props.selectedDatasetId} />
        <UploadConceptListModal selectedDatasetId={this.props.selectedDatasetId} />
        <QueryGroupModal />
      </div>
    );
  }
}

export default QueryEditor;
