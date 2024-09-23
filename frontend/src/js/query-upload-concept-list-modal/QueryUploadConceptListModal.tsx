import { useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";

import { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { TreesT } from "../concept-trees/reducer";
import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import { resetUploadConceptListModal } from "../upload-concept-list-modal/actions";

import { acceptUploadedConceptsOrFilter } from "./actions";

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

  const onAcceptConceptsOrFilter = useCallback(
    (
      label: string,
      resolvedConcepts: string[],
      resolvedFilter?: {
        tableId: string;
        filterId: string;
        value: SelectOptionT[];
      },
    ) =>
      dispatch(
        acceptUploadedConceptsOrFilter({
          andIdx,
          label,
          rootConcepts,
          resolvedConcepts,
          resolvedFilter,
        }),
      ),
    [andIdx, dispatch, rootConcepts],
  );

  return (
    <UploadConceptListModal
      onClose={onCloseModal}
      onAcceptConceptsOrFilter={onAcceptConceptsOrFilter}
    />
  );
};

export default QueryUploadConceptListModal;
