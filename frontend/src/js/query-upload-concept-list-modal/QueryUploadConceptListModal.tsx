import React, { FC } from "react";
import { StateT } from "app-types";
import { useDispatch, useSelector } from "react-redux";

import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import { TreesT } from "../concept-trees/reducer";

import type { QueryUploadConceptListModalStateT } from "./reducer";

import {
  acceptQueryUploadConceptListModal,
  closeQueryUploadConceptListModal,
} from "./actions";

const QueryUploadConceptListModal: FC = () => {
  const context = useSelector<StateT, QueryUploadConceptListModalStateT>(
    (state) => state.queryUploadConceptListModal
  );

  const dispatch = useDispatch();
  const onClose = () => dispatch(closeQueryUploadConceptListModal());
  const onAccept = (label: string, rootConcepts: TreesT, resolved: string[]) =>
    dispatch(
      acceptQueryUploadConceptListModal(
        context.andIdx,
        label,
        rootConcepts,
        resolved
      )
    );

  if (!context.isOpen) return null;

  return <UploadConceptListModal onClose={onClose} onAccept={onAccept} />;
};

export default QueryUploadConceptListModal;
