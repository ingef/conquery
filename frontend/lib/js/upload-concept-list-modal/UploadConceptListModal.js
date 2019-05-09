// @flow

import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import T from "i18n-react";

import Modal from "../modal/Modal";
import InputSelect from "../form-components/InputSelect";
import InputText from "../form-components/InputText";
import ScrollableList from "../scrollable-list/ScrollableList";
import PrimaryButton from "../button/PrimaryButton";
import FaIcon from "../icon/FaIcon";

import type { StateType } from "../app/reducers";
import type { DatasetIdType } from "../dataset/reducer";

import {
  uploadConceptListModalUpdateLabel,
  selectConceptRootNodeAndResolveCodes,
  uploadConceptListModalClose,
  acceptAndCloseUploadConceptListModal
} from "./actions";

const Root = styled("div")`
  padding: 0 0 10px;
`;

const Section = styled("div")`
  padding: 10px 20px;
`;

const Msg = styled("p")`
  margin: 10px 0 5px;
`;

const StyledInputText = styled(InputText)`
  display: block;
  margin-bottom: 15px;
`;

const BigIcon = styled(FaIcon)`
  font-size: 20px;
  margin-right: 10px;
`;
const ErrorIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.red};
`;
const SuccessIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const CenteredIcon = styled(FaIcon)`
  text-align: center;
`;
const StyledPrimaryButton = styled(PrimaryButton)`
  margin-left: 15px;
`;

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
    <Modal
      closeModal={onCloseModal}
      doneButton
      headline={T.translate("uploadConceptListModal.headline")}
    >
      <Root>
        <StyledInputText
          label={T.translate("uploadConceptListModal.label")}
          fullWidth
          input={{
            value: label,
            onChange: onUpdateLabel
          }}
        />
        <InputSelect
          label={T.translate("uploadConceptListModal.selectConceptRootNode")}
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
        <Section>
          {error && (
            <p>
              <ErrorIcon icon="exclamation-circle" />
              {T.translate("uploadConceptListModal.error")}
            </p>
          )}
          {loading && <CenteredIcon icon="spinner" />}
          {resolved && (
            <>
              {hasResolvedItems && (
                <Msg>
                  <SuccessIcon icon="check-circle" />
                  {T.translate("uploadConceptListModal.resolvedCodes", {
                    context: resolvedItemsCount
                  })}
                  <StyledPrimaryButton
                    onClick={() => onAccept(label, resolved.resolvedConcepts)}
                  >
                    {T.translate("uploadConceptListModal.insertNode")}
                  </StyledPrimaryButton>
                </Msg>
              )}
              {hasUnresolvedItems && (
                <>
                  <Msg>
                    <ErrorIcon icon="exclamation-circle" />
                    <span
                      dangerouslySetInnerHTML={{
                        __html: T.translate(
                          "uploadConceptListModal.unknownCodes",
                          {
                            context: unresolvedItemsCount
                          }
                        )
                      }}
                    />
                  </Msg>
                  <ScrollableList
                    maxVisibleItems={3}
                    fullWidth
                    items={resolved.unknownConcepts}
                  />
                </>
              )}
            </>
          )}
        </Section>
      </Root>
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
  onAccept: (filename, resolvedConcepts) =>
    dispatchProps.onAccept(filename, stateProps.rootConcepts, resolvedConcepts)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(UploadConceptListModal);
