import React                  from 'react';
import { QueryGroupModal }    from '../query-group-modal';

import Query                  from './Query';
import StandardQueryNodeModal from './StandardQueryNodeModal';

class QueryEditor extends React.Component {
  render() {
    return (
      <div className="query-editor">
        <Query />
        <StandardQueryNodeModal />
        <QueryGroupModal />
      </div>
    );
  }
}

export default QueryEditor;
