// @flow

import React              from 'react';
import T                  from 'i18n-react';

import { isEmpty }        from '../common/helpers';
import type { PropsType } from './QueryNodeEditor';

export const DescriptionColumn = (props: PropsType) => {
  const { node, editorState } = props;

  const selectedTable = node.tables[editorState.selectedInputTableIdx];

  return (
    <div className="query-node-editor__fixed_column query-node-editor__column">
      <h4>{T.translate('queryNodeEditor.description')}</h4>
      <div className="query-node-editor__column_content">
        <div className="query-node-editor__description">
          {selectedTable != null &&
            editorState.selectedInput != null &&
            !isEmpty(selectedTable.filters[editorState.selectedInput].description) &&

            <span>{selectedTable.filters[editorState.selectedInput].description}</span>
          }
          {selectedTable != null &&
            editorState.selectedInput != null &&
            isEmpty(selectedTable.filters[editorState.selectedInput].description) &&
            <span>{T.translate('queryNodeEditor.noDescriptionProvided')}</span>
          }
          {editorState.selectedInput == null &&
            <span>{T.translate('queryNodeEditor.selectAFilter')}</span>
          }
        </div>
      </div>
    </div>
  );
}
