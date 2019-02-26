// @flow

import React from "react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import T from "i18n-react";
import classnames from "classnames";

import Modal from "../modal/Modal";
import { InputSelect, InputWithLabel } from "../form-components";
import ScrollableList from "../scrollable-list/ScrollableList";
import type { StateType } from "../app/reducers";
import type { DatasetIdType } from "../dataset/reducer";

import {
  uploadConceptListModalUpdateLabel,
  selectConceptRootNodeAndResolveCodes,
  uploadConceptListModalClose,
  acceptAndCloseUploadConceptListModal
} from "./actions";

type PropsType = {
  loading: boolean,
  isModalOpen: boolean,
  label: String,
  availableConceptRootNodes: Object[],
  selectedConceptRootNode: Object,
  selectedDatasetId: DatasetIdType,
  conceptCodesFromFile: Array<string>,
  resolved: Object,
  resolvedItemsCount: number,
  unresolvedItemsCount: number,
  error: Object,

  onCloseModal: Function,
  onAccept: Function,
  onUpdateLabel: Function,
  onSelectConceptRootNode: Function
};

const UploadConceptListModal = (props: PropsType) => {
  if (!props.isModalOpen) return null;

  const {
    label,
    availableConceptRootNodes,
    selectedConceptRootNode,
    selectedDatasetId,
    loading,
    conceptCodesFromFile,
    resolved,
    resolvedItemsCount,
    unresolvedItemsCount,
    error,

    onAccept,
    onUpdateLabel,
    onCloseModal,
    onSelectConceptRootNode
  } = props;

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  return (
    <Modal closeModal={onCloseModal} doneButton>
      <div className="upload-concept-list-modal">
        <h3>{T.translate("uploadConceptListModal.headline")}</h3>
        <InputWithLabel
          label={T.translate("uploadConceptListModal.label")}
          fullWidth
          input={{
            value: label,
            onChange: onUpdateLabel
          }}
        />
        <div className="upload-concept-list-modal__section">
          <label className="input">
            <span className="input-label">
              {T.translate("uploadConceptListModal.selectConceptRootNode")}
            </span>
            <InputSelect
              input={{
                value: selectedConceptRootNode,
                onChange: value =>
                  onSelectConceptRootNode(
                    selectedDatasetId,
                    value,
                    conceptCodesFromFile
                  )
              }}
              options={availableConceptRootNodes.map(x => ({
                value: x.key,
                label: x.value.label
              }))}
              selectProps={{
                isSearchable: true
              }}
            />
          </label>
        </div>
        {error && (
          <div className="upload-concept-list-modal__status upload-concept-list-modal__section">
            <p className="upload-concept-list-modal__error">
              <i className="fa fa-exclamation-circle fa-2x" />
            </p>
            <p>{T.translate("uploadConceptListModal.error")}</p>
          </div>
        )}
        {loading && (
          <div
            className={classnames(
              "upload-concept-list-modal__status",
              "upload-concept-list-modal__section",
              "upload-concept-list-modal__loading"
            )}
          >
            <i className="fa fa-spinner fa-2x" />
          </div>
        )}
        {resolved && (
          <div className="upload-concept-list-modal__status upload-concept-list-modal__section">
            {hasResolvedItems && !hasUnresolvedItems && (
              <p className="upload-concept-list-modal__success">
                <i className="fa fa-check-circle fa-2x" />
              </p>
            )}
            {hasResolvedItems && hasUnresolvedItems && (
              <p className="upload-concept-list-modal__info">
                <i className="fa fa-info-circle fa-2x" />
              </p>
            )}
            {!hasResolvedItems && hasUnresolvedItems && (
              <p className="upload-concept-list-modal__error">
                <i className="fa fa-exclamation-circle fa-2x" />
              </p>
            )}
            {hasResolvedItems && (
              <p>
                {T.translate("uploadConceptListModal.resolvedCodes", {
                  context: resolvedItemsCount
                })}
              </p>
            )}
            {hasUnresolvedItems && (
              <div>
                <p>
                  {T.translate("uploadConceptListModal.unknownCodes", {
                    context: resolved.unknownConcepts.length
                  })}
                </p>
                <div className="upload-concept-list-modal__section">
                  <ScrollableList
                    maxVisibleItems={3}
                    fullWidth
                    items={resolved.unknownConcepts}
                  />
                </div>
              </div>
            )}
          </div>
        )}
        <div className="upload-concept-list-modal__accept">
          <button
            type="button"
            className="btn btn--primary"
            disabled={!hasResolvedItems}
            onClick={() =>
              onAccept(
                label,
                resolved.resolvedConcepts,
                selectedConceptRootNode
              )
            }
          >
            {T.translate("uploadConceptListModal.insertNode")}
          </button>
        </div>
      </div>
    </Modal>
  );
};

const selectUnresolvedItemsCount = state => {
  const { resolved } = state.uploadConceptListModal;

  return resolved && resolved.unknownConcepts && resolved.unknownConcepts.length
    ? resolved.unknownConcepts.length
    : 0;
};

const selectResolvedItemsCount = state => {
  const { resolved } = state.uploadConceptListModal;

  return resolved &&
    resolved.resolvedConcepts &&
    resolved.resolvedConcepts.length
    ? resolved.resolvedConcepts.length
    : 0;
};

const selectAvailableConceptRootNodes = state => {
  const { trees } = state.categoryTrees;

  if (!trees) return null;

  return Object.entries(trees)
    .map(([key, value]) => ({ key, value }))
    .filter(({ key, value }) => value.codeListResolvable)
    .sort((a, b) =>
      a.value.label.toLowerCase().localeCompare(b.value.label.toLowerCase())
    );
};

const mapStateToProps = (state: StateType) => ({
  isModalOpen: state.uploadConceptListModal.isModalOpen,
  label: state.uploadConceptListModal.label,
  conceptCodesFromFile: state.uploadConceptListModal.conceptCodesFromFile,
  availableConceptRootNodes: selectAvailableConceptRootNodes(state),
  selectedConceptRootNode: state.uploadConceptListModal.selectedConceptRootNode,
  loading: state.uploadConceptListModal.loading,
  resolved: state.uploadConceptListModal.resolved,
  resolvedItemsCount: selectResolvedItemsCount(state),
  unresolvedItemsCount: selectUnresolvedItemsCount(state),
  rootConcepts: state.categoryTrees.trees,
  error: state.uploadConceptListModal.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCloseModal: () => dispatch(uploadConceptListModalClose()),
  onUpdateLabel: label => dispatch(uploadConceptListModalUpdateLabel(label)),
  onAccept: (...params) =>
    dispatch(acceptAndCloseUploadConceptListModal(...params)),
  onSelectConceptRootNode: (...params) =>
    dispatch(selectConceptRootNodeAndResolveCodes(...params))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  onAccept: (filename, resolvedConcepts, selectedConceptRootNode) =>
    dispatchProps.onAccept(
      filename,
      stateProps.rootConcepts,
      resolvedConcepts,
      selectedConceptRootNode
    )
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(UploadConceptListModal);
