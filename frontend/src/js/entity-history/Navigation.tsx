import styled from "@emotion/styled";
import { Dispatch, memo, SetStateAction, useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { downloadBlob } from "../common/helpers/downloadBlob";
import WithTooltip from "../tooltip/WithTooltip";

import { EntityIdsList } from "./EntityIdsList";
import type { EntityIdsStatus } from "./History";
import { LoadHistoryDropzone, LoadingPayload } from "./LoadHistoryDropzone";
import { NavigationHeader } from "./NavigationHeader";
import { closeHistory, useUpdateHistorySession } from "./actions";

const Root = styled("div")`
  display: grid;
  grid-template-rows: auto auto 1fr;
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

const BackButton = styled(IconButton)`
  margin: 0 10px 0 20px;
  display: block;
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
`;

const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
  width: 100%;
`;

interface Props {
  className?: string;
  entityIds: string[];
  entityIdsStatus: EntityIdsStatus;
  currentEntityId: string | null;
  currentEntityIndex: number;
  entityStatusOptions: SelectOptionT[];
  setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
  onLoadFromFile: (payload: LoadingPayload) => void;
}

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
  }: Props) => {
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const updateHistorySession = useUpdateHistorySession();
    const onCloseHistory = useCallback(() => {
      dispatch(closeHistory());
    }, [dispatch]);

    const goToPrev = useCallback(() => {
      const prevIdx = Math.max(0, currentEntityIndex - 1);

      updateHistorySession({ entityId: entityIds[prevIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);
    const goToNext = useCallback(() => {
      const nextIdx = Math.min(entityIds.length - 1, currentEntityIndex + 1);

      updateHistorySession({ entityId: entityIds[nextIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);

    useHotkeys("shift+up", goToPrev, [goToPrev]);
    useHotkeys("shift+down", goToNext, [goToNext]);

    const markedCount = useMemo(
      () => Object.values(entityIdsStatus).filter((v) => v.length > 0).length,
      [entityIdsStatus],
    );

    const backButtonWarning =
      markedCount > 0 ? t("history.backButtonWarning") : "";

    return (
      <Root className={className}>
        <WithTooltip text={backButtonWarning}>
          <BackButton frame icon="chevron-left" onClick={onCloseHistory}>
            {t("common.back")}
          </BackButton>
        </WithTooltip>
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
                onClick={() => {
                  const idToRow = (id: string) => [
                    id,
                    entityIdsStatus[id]
                      ? entityIdsStatus[id].map((o) => o.value)
                      : "",
                  ];

                  const csvString = entityIds
                    .map(idToRow)
                    .map((row) => row.join(";"))
                    .join("\n");

                  const blob = new Blob([csvString], {
                    type: "application/csv",
                  });

                  downloadBlob(blob, "list.csv");
                }}
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
