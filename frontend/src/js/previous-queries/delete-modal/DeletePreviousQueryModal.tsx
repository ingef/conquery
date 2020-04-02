import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";

import Modal from "../../modal/Modal";
import PrimaryButton from "../../button/PrimaryButton";
import TransparentButton from "../../button/TransparentButton";
import { deletePreviousQuery } from "../list/actions";
import { deletePreviousQueryModalClose } from "./actions";

type PropsType = {
  queryId: string | number;
  onClose: () => void;
  onDeletePreviousQuery: () => void;
};

const Root = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

const DeletePreviousQueryModal = (props: PropsType) => {
  return (
    !!props.queryId && (
      <Modal onClose={props.onClose}>
        <Root>
          <h3>{T.translate("deletePreviousQueryModal.areYouSure")}</h3>
          <Btn onClick={props.onClose}>{T.translate("common.cancel")}</Btn>
          <PrimaryBtn onClick={props.onDeletePreviousQuery}>
            {T.translate("common.delete")}
          </PrimaryBtn>
        </Root>
      </Modal>
    )
  );
};

const mapStateToProps = state => ({
  queryId: state.deletePreviousQueryModal.queryId
});

const mapDispatchToProps = dispatch => ({
  onClose: () => dispatch(deletePreviousQueryModalClose()),
  onDeletePreviousQuery: (datasetId, queryId) => {
    dispatch(deletePreviousQueryModalClose());
    dispatch(deletePreviousQuery(datasetId, queryId));
  }
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  onDeletePreviousQuery: () =>
    dispatchProps.onDeletePreviousQuery(ownProps.datasetId, stateProps.queryId)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(DeletePreviousQueryModal);
