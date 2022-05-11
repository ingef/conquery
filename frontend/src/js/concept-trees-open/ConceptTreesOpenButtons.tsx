import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { clearSearchQuery } from "../concept-trees/actions";
import { useRootConceptIds } from "../concept-trees/useRootConceptIds";
import WithTooltip from "../tooltip/WithTooltip";

import { closeAllConceptOpen, resetAllConceptOpen } from "./actions";
import { ConceptTreesOpenStateT } from "./reducer";

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
  const { t } = useTranslation();

  const conceptTreesOpen = useSelector<StateT, ConceptTreesOpenStateT>(
    (state) => state.conceptTreesOpen,
  );
  const rootConceptIds = useRootConceptIds();

  const onCloseAllConceptOpen = () =>
    dispatch(closeAllConceptOpen({ rootConceptIds }));
  const onResetAllConceptOpen = () => {
    dispatch(resetAllConceptOpen());
    dispatch(clearSearchQuery());
  };

  const areAllClosed = rootConceptIds.every(
    (id) => conceptTreesOpen[id] === false,
  );

  const hasSearch = useSelector<StateT, boolean>(
    (state) => !!state.conceptTrees.search.result,
  );

  const isCloseAllDisabled = areAllClosed || hasSearch;

  return (
    <Row className={className}>
      <SxWithTooltip text={t("conceptTreesOpen.resetAll")}>
        <SxIconButton frame icon="home" onClick={onResetAllConceptOpen} />
      </SxWithTooltip>
      <SxWithTooltip text={t("conceptTreesOpen.closeAll")}>
        <SxIconButton
          disabled={isCloseAllDisabled}
          frame
          icon="folder-minus"
          onClick={onCloseAllConceptOpen}
        />
      </SxWithTooltip>
    </Row>
  );
};

export default ConceptTreesOpenButtons;
