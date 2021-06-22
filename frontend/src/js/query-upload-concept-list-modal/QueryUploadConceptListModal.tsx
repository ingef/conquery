import { StateT } from "app-types";
import React, { FC } from "react";
import { useDispatch, useSelector } from "react-redux";

import { TreesT } from "../concept-trees/reducer";
import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import { resetUploadConceptListModal } from "../upload-concept-list-modal/actions";

import {
  acceptQueryUploadConceptListModal,
  closeQueryUploadConceptListModal,
} from "./actions";
import type { QueryUploadConceptListModalStateT } from "./reducer";

const QueryUploadConceptListModal: FC = () => {
  const context = useSelector<StateT, QueryUploadConceptListModalStateT>(
    (state) => state.queryUploadConceptListModal,
  );

  const dispatch = useDispatch();
  const onClose = () => {
    dispatch(closeQueryUploadConceptListModal());
    dispatch(resetUploadConceptListModal());
  };
  const onAccept = (
    label: string,
    rootConcepts: TreesT,
    resolvedConcepts: string[],
  ) =>
    dispatch(
      acceptQueryUploadConceptListModal({
        andIdx: context.andIdx,
        label,
        rootConcepts,
        resolvedConcepts,
      }),
    );

  if (!context.isOpen) return null;

  return <UploadConceptListModal onClose={onClose} onAccept={onAccept} />;
};

export default QueryUploadConceptListModal;
