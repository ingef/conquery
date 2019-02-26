// @flow

import React from "react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import T from "i18n-react";
import classnames from "classnames";

import { Modal } from "../modal";
import { ScrollableList } from "../scrollable-list";
import type { StateType } from "../app/reducers";

import { uploadFilterListModalClose } from "./actions";

type PropsType = {
  label: string,
  unresolvedItemsCount: number,
  resolvedItemsCount: number,
  loading: boolean,
  isModalOpen: boolean,
  resolved: Object,
  error: Object,
  onCloseModal: Function,
  onAccept: Function
};

const UploadConceptListModal = (props: PropsType) => {
  if (!props.isModalOpen) return null;

  const {
    label,
    loading,
    resolved,
    unresolvedItemsCount,
    resolvedItemsCount,
    error,
    onAccept,
    onCloseModal
  } = props;

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  return (
    <Modal closeModal={onCloseModal} doneButton>
      <div className="upload-concept-list-modal">
        <h3>{T.translate("uploadFilterListModal.headline")}</h3>
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
                    context: unresolvedItemsCount
                  })}
                </p>
                <div className="upload-concept-list-modal__section">
                  <ScrollableList
                    maxVisibleItems={3}
                    fullWidth
                    items={resolved.unknownCodes}
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
};

const selectResolvedItemsCount = state => {
  const { resolved } = state.uploadFilterListModal;

  return resolved &&
    resolved.resolvedFilter &&
    resolved.resolvedFilter.value &&
    resolved.resolvedFilter.value.length
    ? resolved.resolvedFilter.value.length
    : 0;
};

const selectUnresolvedItemsCount = state => {
  const { resolved } = state.uploadFilterListModal;

  return resolved && resolved.unknownCodes && resolved.unknownCodes.length
    ? resolved.unknownCodes.length
    : 0;
};

const mapStateToProps = (state: StateType) => ({
  isModalOpen: state.uploadFilterListModal.isModalOpen,
  label: state.uploadFilterListModal.label,
  loading: state.uploadFilterListModal.loading,
  resolved: state.uploadFilterListModal.resolved,
  resolvedItemsCount: selectResolvedItemsCount(state),
  unresolvedItemsCount: selectUnresolvedItemsCount(state),
  error: state.uploadFilterListModal.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCloseModal: () => dispatch(uploadFilterListModalClose())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadConceptListModal);
