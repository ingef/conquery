import { useSelector, useDispatch } from "react-redux";

import { ConceptIdT } from "../api/types";
import type { StateT } from "../app/reducers";

import { setConceptOpen } from "./actions";

interface PropsT {
  conceptId: ConceptIdT;
  openInitially?: boolean;
}

export const useOpenableConcept = ({
  conceptId,
  openInitially = false,
}: PropsT) => {
  const conceptOpen = useSelector<StateT, boolean>(
    (state) => state.conceptTreesOpen[conceptId],
  );
  const open = conceptOpen ?? openInitially;

  const dispatch = useDispatch();
  const onToggleOpen = () =>
    dispatch(setConceptOpen({ conceptId, open: !open }));

  return {
    open,
    onToggleOpen,
  };
};
