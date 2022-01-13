import { StateT } from "app-types";
import { useSelector, useDispatch } from "react-redux";

import { ConceptIdT } from "../api/types";

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
  const open = conceptOpen == null ? openInitially : conceptOpen;

  const dispatch = useDispatch();
  const onToggleOpen = () =>
    dispatch(setConceptOpen({ conceptId, open: !open }));

  return {
    open,
    onToggleOpen,
  };
};
