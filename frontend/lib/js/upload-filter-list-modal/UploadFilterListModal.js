// @flow

import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import T from "i18n-react";

import { Modal } from "../modal";
import { ScrollableList } from "../scrollable-list";
import type { StateType } from "../app/reducers";
import FaIcon from "../icon/FaIcon";

import { uploadFilterListModalClose } from "./actions";

const Root = styled("div")`
  padding: 0 0 10px;
`;
const Section = styled("div")`
  padding: 10px 20px;
`;
const Msg = styled("p")`
  margin: 10px 0 5px;
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

type PropsType = {
  unresolvedItemsCount: number,
  resolvedItemsCount: number,
  loading: boolean,
  isModalOpen: boolean,
  resolved: Object,
  error: Object,
  onClose: Function
};

const UploadFilterListModal = (props: PropsType) => {
  if (!props.isModalOpen) return null;

  const {
    loading,
    resolved,
    unresolvedItemsCount,
    resolvedItemsCount,
    error,
    onClose
  } = props;

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={T.translate("uploadFilterListModal.headline")}
    >
      <Root>
        {loading && <CenteredIcon icon="spinner" />}
        {error && (
          <p>
            <ErrorIcon icon="exclamation-circle" />
            {T.translate("uploadConceptListModal.error")}
          </p>
        )}
        {resolved && (
          <Section>
            {hasResolvedItems && (
              <Msg>
                <SuccessIcon icon="check-circle" />
                {T.translate("uploadConceptListModal.resolvedCodes", {
                  context: resolvedItemsCount
                })}
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
                  items={resolved.unknownCodes}
                />
              </>
            )}
          </Section>
        )}
      </Root>
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
  loading: state.uploadFilterListModal.loading,
  resolved: state.uploadFilterListModal.resolved,
  resolvedItemsCount: selectResolvedItemsCount(state),
  unresolvedItemsCount: selectUnresolvedItemsCount(state),
  error: state.uploadFilterListModal.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onClose: () => dispatch(uploadFilterListModalClose())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadFilterListModal);
