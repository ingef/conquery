// @flow

import * as React            from 'react';
import classnames            from 'classnames';

import { Tags }              from '../tags';
import EditableTagsForm      from './EditableTagsForm';

type PropsType = {
  className: string,
  tags?: string[],
  editing: boolean,
  loading: boolean,
  tagComponent?: React.Node,
  onSubmit: () => void,
  onToggleEdit: () => void,
  availableTags: string[]
};

const EditableTags = (props: PropsType) => {
  return props.editing
    ? <EditableTagsForm
        className={classnames(props.className, "editable-tags", "editable-tags__form")}
        tags={props.tags}
        loading={props.loading}
        onSubmit={props.onSubmit}
        onCancel={props.onToggleEdit}
        availableTags={props.availableTags}
      />
    : !!props.tags && props.tags.length > 0 &&
      <div className={classnames(props.className, "editable-tags")}>
        <span
          onClick={props.onToggleEdit}
          className="editable-tags__edit-btn btn--icon edit-btn"
        >
          <i className="fa fa-edit" />
        </span>
        {
          props.tagComponent ||
          <Tags
            className="editable-tags__tags"
            tags={props.tags}
          />
        }
      </div>;
};

export default EditableTags;
