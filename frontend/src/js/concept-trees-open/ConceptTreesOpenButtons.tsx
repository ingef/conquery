import React, { FC } from "react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";
import T from "i18n-react";

import WithTooltip from "../tooltip/WithTooltip";
import IconButton from "../button/IconButton";
import { closeAllConceptOpen, resetAllConceptOpen } from "./actions";
import { useRootConceptIds } from "../concept-trees/useRootConceptIds";
import { ConceptTreesOpenStateT } from "./reducer";
import { StateT } from "app-types";
import { clearSearchQuery } from "../concept-trees/actions";

const SxWithTooltip = styled(WithTooltip)`
  margin-right: 5px;
  &:last-of-type {
    margin-right: 0;
  }
`;
const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const SxIconButton = styled(IconButton)`
  padding: 8px 6px;
`;

interface PropsT {
  className?: string;
}

const ConceptTreesOpenButtons: FC<PropsT> = ({ className }) => {
  const dispatch = useDispatch();

  const conceptTreesOpen = useSelector<StateT, ConceptTreesOpenStateT>(
    (state) => state.conceptTreesOpen
  );
  const rootConceptIds = useRootConceptIds();

  const onCloseAllConceptOpen = () =>
    dispatch(closeAllConceptOpen(rootConceptIds));
  const onResetAllConceptOpen = () => {
    dispatch(resetAllConceptOpen());
    dispatch(clearSearchQuery());
  };

  const areAllClosed = rootConceptIds.every(
    (id) => conceptTreesOpen[id] === false
  );

  const hasSearch = useSelector<StateT, boolean>(
    (state) => !!state.conceptTrees.search.result
  );

  const isCloseAllDisabled = areAllClosed || hasSearch;

  return (
    <Row className={className}>
      <SxWithTooltip text={T.translate("conceptTreesOpen.resetAll")}>
        <SxIconButton frame icon="home" onClick={onResetAllConceptOpen} />
      </SxWithTooltip>
      <SxWithTooltip text={T.translate("conceptTreesOpen.closeAll")}>
        <SxIconButton
          disabled={isCloseAllDisabled}
          frame
          icon="folder"
          onClick={onCloseAllConceptOpen}
        />
      </SxWithTooltip>
    </Row>
  );
};

export default ConceptTreesOpenButtons;
