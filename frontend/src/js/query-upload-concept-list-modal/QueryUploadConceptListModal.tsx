import { useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { TreesT } from "../concept-trees/reducer";
import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import { resetUploadConceptListModal } from "../upload-concept-list-modal/actions";

import { acceptQueryUploadConceptListModal } from "./actions";

const QueryUploadConceptListModal = ({
  andIdx,
  onClose,
}: {
  andIdx?: number;
  onClose: () => void;
}) => {
  const dispatch = useDispatch();
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  const onCloseModal = useCallback(() => {
    dispatch(resetUploadConceptListModal());
    onClose();
  }, [dispatch, onClose]);

  const onAcceptConcepts = useCallback(
    (label: string, resolvedConcepts: string[]) =>
      dispatch(
        acceptQueryUploadConceptListModal({
          andIdx,
          label,
          rootConcepts,
          resolvedConcepts,
        }),
      ),
    [andIdx, dispatch, rootConcepts],
  );

  const onAcceptFilters = useCallback(
    () => console.log("FILTER"),
    // dispatch(
    //   acceptQueryUploadConceptListModal({
    //     andIdx,
    //     label,
    //     rootConcepts,
    //     resolvedConcepts,
    //   }),
    // ),
    [],
  );

  return (
    <UploadConceptListModal
      onClose={onCloseModal}
      onAcceptConcepts={onAcceptConcepts}
      onAcceptFilters={onAcceptFilters}
    />
  );
};

export default QueryUploadConceptListModal;
