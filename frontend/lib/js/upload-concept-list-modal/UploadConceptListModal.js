// @flow

import React                     from 'react';
import type { Dispatch }         from 'redux-thunk';
import { connect }               from 'react-redux';
import T                         from 'i18n-react';
import classnames                from 'classnames';

import { Modal }                 from '../modal';
import {
  InputSelect,
  InputWithLabel
}                                from '../form-components';
import { ScrollableList }        from '../scrollable-list';
import type { StateType }        from '../app/reducers';
import type { DatasetIdType }    from '../dataset/reducer';

import {
  uploadConceptListModalUpdateLabel,
  selectConceptRootNodeAndResolveCodes,
  uploadConceptListModalClose,
  acceptAndCloseUploadConceptListModal
}                                from './actions'

type PropsType = {
  onCloseModal: Function,
  onAccept: Function,
  loading: boolean,
  isModalOpen: boolean,
  showDetails?: boolean,
  parameters: Object,
  label: String,
  availableConceptRootNodes: Array,
  selectedConceptRootNode: Object,
  selectConceptRootNode: Function,
  selectedDatasetId: DatasetIdType,
  conceptCodesFromFile: Array<string>,
  updateLabel: Function,
  resolved: Object,
  hasResolvedItems: boolean,
  hasUnresolvedCodes: boolean,
  numberOfResolvedItems: Number,
  error: Object
};

const UploadConceptListModal = (props: PropsType) => {
  const {
    isModalOpen,
    showDetails,
    availableConceptRootNodes,
    selectedConceptRootNode,
    loading,
    resolved,
    hasResolvedItems,
    hasUnresolvedCodes,
    numberOfResolvedItems,
    error,
    parameters
  } = props;

  if (!isModalOpen && resolved && resolved.resolvedFilter)
    props.onAccept(props.label, {filter: resolved.resolvedFilter}, parameters);

  if (!isModalOpen) return null;

  return (
    <Modal closeModal={props.onCloseModal} doneButton>
      <div className="upload-concept-list-modal">
        <h3>
          { T.translate('uploadConceptListModal.headline') }
        </h3>
        {
          showDetails &&
          <InputWithLabel
            label={T.translate('uploadConceptListModal.label')}
            fullWidth
            input={{
              value: props.label,
              onChange: (value) => props.updateLabel(value)
            }}
          />
        }
        { showDetails &&
          <div className="upload-concept-list-modal__section">
            <label className="input">
              <span className="input-label">
                { T.translate('uploadConceptListModal.selectConceptRootNode') }
              </span>
              <InputSelect
                input={{
                  value: selectedConceptRootNode,
                  onChange: (value) =>
                      props.selectConceptRootNode(
                          props.selectedDatasetId,
                          value,
                          props.conceptCodesFromFile,
                          parameters
                      )
                }}
                options={
                  availableConceptRootNodes
                    .map(x => ({ value: x.key, label: x.value.label }))
                }
                selectProps={{
                  isSearchable: true
                }}
              />
            </label>
          </div>
        }
        {
          error &&
          <div className="upload-concept-list-modal__status upload-concept-list-modal__section">
            <p className="upload-concept-list-modal__error">
              <i className="fa fa-exclamation-circle fa-2x" />
            </p>
            <p>
              { T.translate('uploadConceptListModal.error') }
            </p>
          </div>
        }
        {
          loading &&
          <div className={classnames(
              'upload-concept-list-modal__status',
              'upload-concept-list-modal__section',
              'upload-concept-list-modal__loading'
            )}
          >
            <i className="fa fa-spinner fa-2x" />
          </div>
        }
        {
          resolved &&
          <div className="upload-concept-list-modal__status upload-concept-list-modal__section">
            {
              hasResolvedItems &&
              !hasUnresolvedCodes &&
              <p className="upload-concept-list-modal__success">
                <i className="fa fa-check-circle fa-2x" />
              </p>
            }
            {
              hasResolvedItems &&
              hasUnresolvedCodes &&
              <p className="upload-concept-list-modal__info">
                <i className="fa fa-info-circle fa-2x" />
              </p>
            }
            {
              !hasResolvedItems &&
              hasUnresolvedCodes &&
              <p className="upload-concept-list-modal__error">
                <i className="fa fa-exclamation-circle fa-2x" />
              </p>
            }
            {
              hasResolvedItems &&
              <p>
                {
                  T.translate(
                    'uploadConceptListModal.resolvedCodes',
                    { context: numberOfResolvedItems }
                  )
                }
              </p>
            }
            {
              hasUnresolvedCodes &&
              <div>
                <p>
                  {
                    T.translate(
                      'uploadConceptListModal.unknownCodes',
                      { context: resolved.unknownCodes.length }
                    )
                  }
                </p>
                <div className="upload-concept-list-modal__section">
                  <ScrollableList
                    maxVisibleItems={3}
                    fullWidth
                    items={resolved.unknownCodes}
                  />
                </div>
              </div>
            }
          </div>
        }
        <div className="upload-concept-list-modal__accept">
          <button
            type="button"
            className="btn btn--primary"
            disabled={!hasResolvedItems}
            onClick={() => props.onAccept(
              props.label,
              {
                conceptList: props.resolved.resolvedConcepts,
                filter: props.resolved.resolvedFilter,
                selectedRoot: props.selectedConceptRootNode
              },
              props.parameters
            )}
          >
            { T.translate('uploadConceptListModal.insertNode') }
          </button>
        </div>
      </div>
    </Modal>
  );
}

const selectHasResolvedConcepts = (state) => {
  const { resolved } = state.uploadConceptListModal;

  return resolved && resolved.resolvedConcepts && resolved.resolvedConcepts.length;
};

const selectHasResolvedFilters = (state) => {
  const { resolved } = state.uploadConceptListModal;

  return resolved &&
    resolved.resolvedFilter &&
    resolved.resolvedFilter.value &&
    resolved.resolvedFilter.value.length;
}

const selectHasResolvedItems = (state) => {
  return selectHasResolvedConcepts(state) || selectHasResolvedFilters(state);
}

const selectHasUnresolvedCodes = (state) => {
  const { resolved } = state.uploadConceptListModal;

  return resolved && resolved.unknownCodes && resolved.unknownCodes.length;
}

const selectNumberOfResolvedItems = (state) => {
  const { resolved } = state.uploadConceptListModal;

  if (!selectHasResolvedItems(state))
    return 0;

  if (selectHasResolvedConcepts(state))
    return resolved.resolvedConcepts.length;
  else
    return resolved.resolvedFilter.value.length;
}

const selectAvailableConceptRootNodes = (state) => {
  const { trees } = state.categoryTrees;

  if (!trees) return null;

  return Object.entries(trees)
    .map(([key, value]) => ({ key, value }))
    .filter(({ key, value }) => value.codeListResolvable)
    .sort((a, b) => a.value.label.toLowerCase().localeCompare(b.value.label.toLowerCase()));
};

const selectShowDetails = (state) => {
  const { showDetails } = state.uploadConceptListModal;

  if (typeof showDetails === 'undefined') return true;

  return showDetails;
}

const mapStateToProps = (state: StateType) => ({
  isModalOpen: state.uploadConceptListModal.isModalOpen,
  showDetails: selectShowDetails(state),
  parameters: state.uploadConceptListModal.parameters,
  label: state.uploadConceptListModal.label,
  conceptCodesFromFile: state.uploadConceptListModal.conceptCodesFromFile,
  availableConceptRootNodes: selectAvailableConceptRootNodes(state),
  selectedConceptRootNode: state.uploadConceptListModal.selectedConceptRootNode,
  loading: state.uploadConceptListModal.loading,
  resolved: state.uploadConceptListModal.resolved,
  hasResolvedItems: selectHasResolvedItems(state),
  hasUnresolvedCodes: selectHasUnresolvedCodes(state),
  numberOfResolvedItems: selectNumberOfResolvedItems(state),
  rootConcepts: state.categoryTrees.trees,
  error: state.uploadConceptListModal.error,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCloseModal: () => dispatch(uploadConceptListModalClose()),
  onAccept: (label, rootConcepts, concepts, parameters) =>
    dispatch(acceptAndCloseUploadConceptListModal(label, rootConcepts, concepts, parameters)),
  selectConceptRootNode: (datasetId, treeId, conceptCodes, parameters) =>
    dispatch(selectConceptRootNodeAndResolveCodes(
      { ...parameters, datasetId, treeId, conceptCodes }
    )),
  updateLabel: (label) => dispatch(uploadConceptListModalUpdateLabel(label))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  onAccept: (fileName, concepts, parameters) =>
    dispatchProps.onAccept(
      fileName,
      stateProps.rootConcepts,
      concepts,
      parameters
    ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(UploadConceptListModal);
