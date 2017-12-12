import React, { PropTypes }  from 'react';
import classnames            from 'classnames';

import { Tags }              from '../tags';
import EditableTagsForm      from './EditableTagsForm';

const EditableTags = (props) => {
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

EditableTags.propTypes = {
  className: PropTypes.string,
  tags: PropTypes.arrayOf(PropTypes.string),
  editing: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  tagComponent: PropTypes.element,
  onSubmit: PropTypes.func.isRequired,
  onToggleEdit: PropTypes.func.isRequired,
  availableTags: PropTypes.arrayOf(PropTypes.string),
}

export default EditableTags;
