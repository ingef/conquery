// @flow

import React                from 'react';

import { SelectableLabel }  from '../selectable-label';
import EditableTextForm     from './EditableTextForm';

type PropsType = {
  className?: string,
  loading: boolean,
  editing: boolean,
  text: string,
  selectTextOnMount: boolean,
  onSubmit: () => void,
  onToggleEdit: () => void,
};

class EditableText extends React.Component<PropsType> {
  render() {
    return this.props.editing
      ? <EditableTextForm
          className={this.props.className}
          loading={this.props.loading}
          text={this.props.text}
          selectTextOnMount={this.props.selectTextOnMount}
          onSubmit={this.props.onSubmit}
          onCancel={this.props.onToggleEdit}
        />
      : <p className={this.props.className}>
          <span>
            <span
              className="editable-text__edit-icon btn--icon edit-btn"
              onClick={this.props.onToggleEdit}
            >
              <i className="fa fa-edit" />
            </span>
            <SelectableLabel label={this.props.text} />
          </span>
        </p>;
  }
};

export default EditableText;
