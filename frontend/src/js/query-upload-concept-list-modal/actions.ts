import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
} from "../upload-concept-list-modal/actions";

import { MODAL_OPEN, MODAL_CLOSE, MODAL_ACCEPT } from "./actionTypes";

const openModal = (andIdx: number | null = null) => ({
  type: MODAL_OPEN,
  payload: { andIdx },
});

export const openQueryUploadConceptListModal = (
  andIdx: number | null,
  file: File,
) => async (dispatch) => {
  // Need to wait until file is processed.
  // Because if file is empty, modal would close automatically
  await dispatch(initUploadConceptListModal(file));

  return dispatch(openModal(andIdx));
};

const closeModal = () => ({
  type: MODAL_CLOSE,
});

export const closeQueryUploadConceptListModal = () => (dispatch) => {
  dispatch(closeModal());
  dispatch(resetUploadConceptListModal());
};

export const acceptQueryUploadConceptListModal = (
  andIdx,
  label,
  rootConcepts,
  resolvedConcepts,
) => {
  return {
    type: MODAL_ACCEPT,
    payload: { andIdx, label, rootConcepts, resolvedConcepts },
  };
};
