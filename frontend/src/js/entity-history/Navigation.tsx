import styled from "@emotion/styled";
import { Dispatch, memo, SetStateAction, useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SelectOptionT } from "../api/types";
import { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";

import { EntityIdsList } from "./EntityIdsList";
import type { EntityIdsStatus } from "./History";
import { LoadHistoryDropzone, LoadingPayload } from "./LoadHistoryDropzone";
import { NavigationHeader } from "./NavigationHeader";
import { closeHistory, resetHistory, useUpdateHistorySession } from "./actions";
import { EntityId } from "./reducer";
import { saveHistory } from "./saveAndLoad";

const Root = styled("div")`
  display: grid;
  gap: 10px;
  overflow: hidden;
  background-color: ${({ theme }) => theme.col.bg};
`;

const EntityIdNav = styled("div")`
  display: grid;
  grid-template-rows: auto 12fr auto auto;
  overflow: hidden;
  padding: 0 10px 0 20px;
`;
const TopActions = styled("div")`
  display: flex;
`;

const SxNavigationHeader = styled(NavigationHeader)`
  margin: 0 10px 0 20px;
`;

const SxLoadHistoryDropzone = styled(LoadHistoryDropzone)`
  height: 100%;
  overflow-y: auto;
  padding: 2px;
  display: block;
  color: inherit;
`;
const BottomActions = styled("div")`
  display: flex;
`;

const ContainedIconButton = styled(IconButton)`
  margin: 0 10px 0 20px;
  justify-content: center;
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
  justify-content: center;
`;

const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
  width: 100%;
`;

export const Navigation = memo(
  ({
    className,
    entityIds,
    entityIdsStatus,
    currentEntityId,
    currentEntityIndex,
    entityStatusOptions,
    setEntityStatusOptions,
    onLoadFromFile,
  }: {
    className?: string;
    entityIds: EntityId[];
    entityIdsStatus: EntityIdsStatus;
    currentEntityId: EntityId | null;
    currentEntityIndex: number;
    entityStatusOptions: SelectOptionT[];
    setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
    onLoadFromFile: (payload: LoadingPayload) => void;
  }) => {
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const updateHistorySession = useUpdateHistorySession();
    const onCloseHistory = useCallback(() => {
      dispatch(closeHistory());
    }, [dispatch]);

    const ids = useSelector<StateT, unknown[]>(
      (state) => state.entityHistory.entityIds,
    );

    const goToPrev = useCallback(() => {
      const prevIdx = Math.max(0, currentEntityIndex - 1);

      updateHistorySession({ entityId: entityIds[prevIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);
    const goToNext = useCallback(() => {
      const nextIdx = Math.min(entityIds.length - 1, currentEntityIndex + 1);

      updateHistorySession({ entityId: entityIds[nextIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);

    const onDownload = useCallback(() => {
      saveHistory({ entityIds, entityIdsStatus });
    }, [entityIds, entityIdsStatus]);

    const onReset = useCallback(() => {
      dispatch(resetHistory());
    }, [dispatch]);

    useHotkeys("shift+up", goToPrev, [goToPrev]);
    useHotkeys("shift+down", goToNext, [goToNext]);

    const markedCount = useMemo(
      () => Object.values(entityIdsStatus).filter((v) => v.length > 0).length,
      [entityIdsStatus],
    );

    const backButtonWarning =
      markedCount > 0 ? t("history.backButtonWarning") : "";

    const showResetButton = ids.length > 0;

    return (
      <Root
        className={className}
        style={{
          gridTemplateRows: showResetButton
            ? "auto auto auto 1fr"
            : "auto auto 1fr",
        }}
      >
        <WithTooltip text={backButtonWarning}>
          <ContainedIconButton
            frame
            icon="chevron-left"
            onClick={onCloseHistory}
          >
            {t("common.back")}
          </ContainedIconButton>
        </WithTooltip>
        {ids.length > 0 && (
          <ConfirmableTooltip
            onConfirm={onReset}
            confirmationText={t("history.settings.resetConfirm")}
          >
            <ContainedIconButton frame icon="trash">
              {t("history.settings.reset")}
            </ContainedIconButton>
          </ConfirmableTooltip>
        )}
        <SxNavigationHeader
          markedCount={markedCount}
          idsCount={entityIds.length}
          entityStatusOptions={entityStatusOptions}
          setEntityStatusOptions={setEntityStatusOptions}
        />
        <EntityIdNav>
          <TopActions>
            <SxWithTooltip
              text={`${t("history.prevButtonLabel")} (shift + ⬆)`}
              lazy
            >
              <SxIconButton icon="arrow-up" onClick={goToPrev} />
            </SxWithTooltip>
          </TopActions>
          <SxLoadHistoryDropzone onLoadFromFile={onLoadFromFile}>
            <EntityIdsList
              currentEntityId={currentEntityId}
              entityIds={entityIds}
              updateHistorySession={updateHistorySession}
              entityIdsStatus={entityIdsStatus}
            />
          </SxLoadHistoryDropzone>
          <BottomActions>
            <SxWithTooltip
              text={`${t("history.nextButtonLabel")} (shift + ⬇)`}
              lazy
            >
              <SxIconButton icon="arrow-down" onClick={goToNext} />
            </SxWithTooltip>
          </BottomActions>
          <BottomActions style={{ marginTop: "10px" }}>
            <SxWithTooltip text={t("history.downloadButtonLabel")}>
              <SxIconButton
                style={{ backgroundColor: "white" }}
                frame
                icon="download"
                onClick={onDownload}
              >
                CSV
              </SxIconButton>
            </SxWithTooltip>
          </BottomActions>
        </EntityIdNav>
      </Root>
    );
  },
);
