import React, { PropTypes } from 'react';
import T                    from 'i18n-react';
import { DragSource }       from 'react-dnd';
import { connect }          from 'react-redux';
import moment               from 'moment';
import classnames           from 'classnames';


import { ErrorMessage }     from '../../error-message';
import { dndTypes }         from '../../common/constants';
import { SelectableLabel }  from '../../selectable-label';

import {
  CloseIconButton,
  DownloadButton,
}  from '../../button';

import {
  EditableText,
  EditableTags
}                           from '../../form-components';

import {
  deletePreviousQueryModalOpen,
}                           from '../delete-modal/actions'

import {
  sharePreviousQuery,
  renamePreviousQuery,
  retagPreviousQuery,
  toggleEditPreviousQueryLabel,
  toggleEditPreviousQueryTags,
}                           from './actions';

import PreviousQueryTags    from './PreviousQueryTags';

const nodeSource = {
  beginDrag(props) {
    // Return the data describing the dragged item
    return {
      id: props.query.id,
      label: props.query.label,
      isPreviousQuery: true,
    };
  }
};

// These props get injected into the component
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}

const PreviousQuery = (props) => {
  const {
    query,
    onRenamePreviousQuery,
    onToggleEditPreviousQueryTags,
    onToggleEditPreviousQueryLabel,
    onRetagPreviousQuery,
    onSharePreviousQuery,
  } = props;

  const peopleFound = `${query.numberOfResults} ${T.translate('previousQueries.results')}`;
  const executedAt = moment(query.createdAt).fromNow();
  const label = query.label || query.id.toString();
  const mayEditQuery = query.own || query.shared;

  return props.connectDragSource(
    <div className={classnames('previous-query', {
      'previous-query--own': !!query.own,
      'previous-query--shared': !!query.shared,
      'previous-query--system': !!query.system || (!query.own && !query.shared),
    })}>
      <p className="previous-query__top-infos">
        <span className="previous-query__top-left">
          {
            query.resultUrl
              ? <DownloadButton
                  className="btn--icon"
                  url={query.resultUrl}
                  label={peopleFound}
                />
              : peopleFound
          }
          {
            query.own && (
            query.shared
            ? <span className="previous-query__shared-indicator">
                { T.translate('previousQuery.shared') }
              </span>
            : <span
                onClick={onSharePreviousQuery}
                className="previous-query__btn btn--icon"
              >
                <i className="fa fa-upload" /> { T.translate('previousQuery.share') }
              </span>
          )
          }
          {
            mayEditQuery && !query.editingTags && (!query.tags || query.tags.length === 0) &&
            <span
              onClick={onToggleEditPreviousQueryTags}
              className="previous-query__btn previous-query__hover-btn btn--icon"
            >
              <i className="fa fa-plus" /> { T.translate('previousQuery.addTag') }
            </span>
          }
        </span>
        <span className="previous-query__top-right">
          { executedAt }
          {
            query.loading
              ? <span className="btn--icon--padded fa fa-spinner" />
              : query.own && <CloseIconButton onClick={props.onDeletePreviousQuery} />
          }
        </span>
      </p>
      <div className="previous-query__middle-row">
        {
          mayEditQuery
          ? <EditableText
              className="previous-query__label"
              loading={!!query.loading}
              text={label}
              selectTextOnMount={true}
              editing={!!query.editingLabel}
              onSubmit={onRenamePreviousQuery}
              onToggleEdit={onToggleEditPreviousQueryLabel}
            />
          : <SelectableLabel className="previous-query__label" label={label} />
        }
        <span className="previous-query__top-infos">
          { query.ownerName }
        </span>
      </div>
      {
        mayEditQuery
        ? <EditableTags
            className="previous-query__tags"
            tags={query.tags}
            editing={!!query.editingTags}
            loading={!!query.loading}
            onSubmit={onRetagPreviousQuery}
            onToggleEdit={onToggleEditPreviousQueryTags}
            tagComponent={
              <PreviousQueryTags
                className="editable-tags__tags"
                tags={query.tags}
              />
            }
            availableTags={props.availableTags}
          />
        : <PreviousQueryTags
            className="previous-query__tags editable-tags__tags"
            tags={query.tags}
          />
      }
      {
        !!query.error &&
        <ErrorMessage
          className="previous-query__error"
          message={query.error}
        />
      }
    </div>
  );
};

PreviousQuery.propTypes = {
  query: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    label: PropTypes.string,
    loading: PropTypes.bool,
    numberOfResults: PropTypes.number.isRequired,
    createdAt: PropTypes.string.isRequired,
    tags: PropTypes.arrayOf(PropTypes.string),
    own: PropTypes.bool,
    shared: PropTypes.bool,
  }).isRequired,
  onRenamePreviousQuery: PropTypes.func.isRequired,
  onToggleEditPreviousQueryLabel: PropTypes.func.isRequired,
  onToggleEditPreviousQueryTags: PropTypes.func.isRequired,
  onSharePreviousQuery: PropTypes.func.isRequired,
  onRetagPreviousQuery: PropTypes.func.isRequired,
  onDeletePreviousQuery: PropTypes.func.isRequired,
  connectDragSource: PropTypes.func.isRequired,
  availableTags: PropTypes.arrayOf(PropTypes.string),
};

const mapStateToProps = (state) => ({
    availableTags: state.previousQueries.tags,
});

const mapDispatchToProps = (dispatch) => ({
  onSharePreviousQuery: (datasetId, queryId) =>
    dispatch(sharePreviousQuery(datasetId, queryId)),

  onRenamePreviousQuery: (datasetId, queryId, label) =>
    dispatch(renamePreviousQuery(datasetId, queryId, label)),

  onRetagPreviousQuery: (datasetId, queryId, tags) =>
    dispatch(retagPreviousQuery(datasetId, queryId, tags)),

  onDeletePreviousQuery: (queryId) =>
    dispatch(deletePreviousQueryModalOpen(queryId)),

  onToggleEditPreviousQueryLabel: (queryId) =>
    dispatch(toggleEditPreviousQueryLabel(queryId)),

  onToggleEditPreviousQueryTags: (queryId) =>
    dispatch(toggleEditPreviousQueryTags(queryId)),
});

const mapProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  onSharePreviousQuery: () =>
    dispatchProps.onSharePreviousQuery(ownProps.datasetId, ownProps.query.id),
  onRenamePreviousQuery: (label) =>
    dispatchProps.onRenamePreviousQuery(ownProps.datasetId, ownProps.query.id, label),
  onRetagPreviousQuery: (tags) =>
    dispatchProps.onRetagPreviousQuery(ownProps.datasetId, ownProps.query.id, tags),
  onDeletePreviousQuery: () =>
    dispatchProps.onDeletePreviousQuery(ownProps.query.id),
  onToggleEditPreviousQueryLabel: () =>
    dispatchProps.onToggleEditPreviousQueryLabel(ownProps.query.id),
  onToggleEditPreviousQueryTags: () =>
    dispatchProps.onToggleEditPreviousQueryTags(ownProps.query.id),
});

export default DragSource(
  dndTypes.PREVIOUS_QUERY,
  nodeSource,
  collect
)(connect(mapStateToProps, mapDispatchToProps, mapProps)(PreviousQuery));
