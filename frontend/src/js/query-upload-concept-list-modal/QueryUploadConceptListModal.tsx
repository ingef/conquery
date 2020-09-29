import React from "react";
import { connect } from "react-redux";

import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";

import {
  acceptQueryUploadConceptListModal,
  closeQueryUploadConceptListModal
} from "./actions";

export default connect(
  state => ({ context: state.queryUploadConceptListModal }),
  dispatch => ({
    accept: (...params) =>
      dispatch(acceptQueryUploadConceptListModal(...params)),
    onClose: () => dispatch(closeQueryUploadConceptListModal())
  })
)(({ accept, context, ...props }) => {
  if (!context.isOpen) return null;

  const onAccept = (label, rootConcepts, resolved) =>
    accept(context.andIdx, label, rootConcepts, resolved);

  return <UploadConceptListModal {...props} onAccept={onAccept} />;
});
