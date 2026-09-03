import { faFolderMinus, faHome } from "@fortawesome/free-solid-svg-icons";
import { memo, useCallback, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { clearSearchQuery } from "../concept-trees/actions";
import { useRootConceptIds } from "../concept-trees/useRootConceptIds";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import { closeAllConceptOpen, resetAllConceptOpen } from "./actions";
import type { ConceptTreesOpenStateT } from "./reducer";

const row = tv({ base: ["flex items-center", "gap-[5px]"] });

const ConceptTreesOpenButtons = ({ className }: { className?: string }) => {
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
      <div className={row({ className })}>
        <TooltipTrigger>
          <Button
            aria-label={t("conceptTreesOpen.resetAll")}
            intent="secondary"
            onPress={onResetAllConceptOpen}
          >
            <Icon icon={faHome} />
          </Button>
          <Tooltip>{t("conceptTreesOpen.resetAll")}</Tooltip>
        </TooltipTrigger>
        <TooltipTrigger>
          <Button
            aria-label={t("conceptTreesOpen.closeAll")}
            intent="secondary"
            isDisabled={isCloseAllDisabled}
            onPress={onCloseAllConceptOpen}
          >
            <Icon icon={faFolderMinus} />
          </Button>
          <Tooltip>{t("conceptTreesOpen.closeAll")}</Tooltip>
        </TooltipTrigger>
      </div>
    );
  },
);

export default ConceptTreesOpenButtons;
