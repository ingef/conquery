import styled from "@emotion/styled";
import { FC, memo, useCallback, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { clearSearchQuery } from "../concept-trees/actions";
import { useRootConceptIds } from "../concept-trees/useRootConceptIds";
import WithTooltip from "../tooltip/WithTooltip";

import { closeAllConceptOpen, resetAllConceptOpen } from "./actions";
import { ConceptTreesOpenStateT } from "./reducer";

const Row = styled("div")`
  display: flex;
  align-items: center;
  gap: 5px;
`;

const SxIconButton = styled(IconButton)`
  padding: 9px 6px;
`;

interface PropsT {
  className?: string;
}

const ConceptTreesOpenButtons: FC<PropsT> = ({ className }) => {
  const dispatch = useDispatch();

  const conceptTreesOpen = useSelector<StateT, ConceptTreesOpenStateT>(
    (state) => state.conceptTreesOpen,
  );
  const rootConceptIds = useRootConceptIds();
  const rootConceptIdsRef = useRef(rootConceptIds);
  rootConceptIdsRef.current = rootConceptIds;

  const onCloseAllConceptOpen = useCallback(() => {
    dispatch(
      closeAllConceptOpen({ rootConceptIds: rootConceptIdsRef.current }),
    );
  }, [dispatch]);
  const onResetAllConceptOpen = useCallback(() => {
    dispatch(resetAllConceptOpen());
    dispatch(clearSearchQuery());
  }, [dispatch]);

  const areAllClosed = rootConceptIds.every(
    (id) => conceptTreesOpen[id] === false,
  );

  const hasSearch = useSelector<StateT, boolean>(
    (state) => !!state.conceptTrees.search.result,
  );

  const isCloseAllDisabled = areAllClosed || hasSearch;

  return (
    <ConceptTreesOpenButtonsView
      className={className}
      isCloseAllDisabled={isCloseAllDisabled}
      onCloseAllConceptOpen={onCloseAllConceptOpen}
      onResetAllConceptOpen={onResetAllConceptOpen}
    />
  );
};

const ConceptTreesOpenButtonsView = memo(
  ({
    className,
    isCloseAllDisabled,
    onResetAllConceptOpen,
    onCloseAllConceptOpen,
  }: {
    className?: string;
    isCloseAllDisabled: boolean;
    onResetAllConceptOpen: () => void;
    onCloseAllConceptOpen: () => void;
  }) => {
    const { t } = useTranslation();

    return (
      <Row className={className}>
        <WithTooltip text={t("conceptTreesOpen.resetAll")}>
          <SxIconButton frame icon="home" onClick={onResetAllConceptOpen} />
        </WithTooltip>
        <WithTooltip text={t("conceptTreesOpen.closeAll")}>
          <SxIconButton
            disabled={isCloseAllDisabled}
            frame
            icon="folder-minus"
            onClick={onCloseAllConceptOpen}
          />
        </WithTooltip>
      </Row>
    );
  },
);

export default ConceptTreesOpenButtons;
