import styled from "@emotion/styled";
import { useCallback, useMemo, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";

import { DetailControl, DetailLevel } from "./DetailControl";
import { DownloadEntityDataButton } from "./DownloadEntityDataButton";
import { EntityHeader } from "./EntityHeader";
import type { LoadingPayload } from "./LoadHistoryDropzone";
import { Navigation } from "./Navigation";
import { Timeline } from "./Timeline";
import { useUpdateHistorySession } from "./actions";

const FullScreen = styled("div")`
  position: fixed;
  top: 0;
  left: 0;
  height: 100%;
  width: 100%;
  z-index: 2;
  background-color: ${({ theme }) => theme.col.bgAlt};
`;

const SxNavigation = styled(Navigation)`
  height: 100%;
  padding: 55px 0 10px;
`;

const SxEntityHeader = styled(EntityHeader)`
  grid-area: header;
`;
const SxTimeline = styled(Timeline)`
  grid-area: timeline;
`;

const Controls = styled("div")`
  grid-area: control;
  justify-self: end;

  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 20px;
`;

const Main = styled("div")`
  overflow: hidden;
  height: 100%;
  display: grid;
  gap: 10px 0;
  grid-template-areas: "header control" "timeline timeline";
  grid-template-rows: auto 1fr;
  padding: 55px 0 10px;
`;

export interface EntityIdsStatus {
  [entityId: string]: SelectOptionT[];
}

export const History = () => {
  const entityIds = useSelector<StateT, string[]>(
    (state) => state.entityHistory.entityIds,
  );
  const currentEntityId = useSelector<StateT, string | null>(
    (state) => state.entityHistory.currentEntityId,
  );
  const [detailLevel, setDetailLevel] = useState<DetailLevel>("summary");
  const updateHistorySession = useUpdateHistorySession();

  const currentEntityIndex = useMemo(() => {
    return currentEntityId ? entityIds.indexOf(currentEntityId) : 0;
  }, [currentEntityId, entityIds]);

  const {
    entityStatusOptions,
    setEntityStatusOptions,
    entityIdsStatus,
    setEntityIdsStatus,
    currentEntityStatus,
    setCurrentEntityStatus,
  } = useEntityStatus({ currentEntityId });

  const onLoad = useCallback(
    ({
      label,
      loadedEntityIds,
      loadedEntityStatus,
      loadedEntityStatusOptions,
    }: LoadingPayload) => {
      updateHistorySession({
        label,
        entityIds: loadedEntityIds,
        entityId: loadedEntityIds[0],
      });
      setEntityIdsStatus(loadedEntityStatus);
      setEntityStatusOptions(loadedEntityStatusOptions);
    },
    [],
  );

  return (
    <FullScreen>
      <SplitPane
        split="vertical"
        minSize={200}
        maxSize={-300}
        defaultSize="20%"
      >
        <SxNavigation
          entityIds={entityIds}
          entityIdsStatus={entityIdsStatus}
          currentEntityId={currentEntityId}
          currentEntityIndex={currentEntityIndex}
          entityStatusOptions={entityStatusOptions}
          setEntityStatusOptions={setEntityStatusOptions}
          onLoad={onLoad}
        />
        <Main>
          {currentEntityId && (
            <SxEntityHeader
              currentEntityIndex={currentEntityIndex}
              currentEntityId={currentEntityId}
              status={currentEntityStatus}
              setStatus={setCurrentEntityStatus}
              entityStatusOptions={entityStatusOptions}
            />
          )}
          <Controls>
            <DetailControl
              detailLevel={detailLevel}
              setDetailLevel={setDetailLevel}
            />
            <DownloadEntityDataButton />
          </Controls>
          <ErrorBoundary
            fallbackRender={() => {
              return <div>Something went wrong here.</div>;
            }}
          >
            <SxTimeline detailLevel={detailLevel} />
          </ErrorBoundary>
        </Main>
      </SplitPane>
    </FullScreen>
  );
};

const useEntityStatus = ({
  currentEntityId,
}: {
  currentEntityId: string | null;
}) => {
  const [entityStatusOptions, setEntityStatusOptions] = useState<
    SelectOptionT[]
  >([]);

  const [entityIdsStatus, setEntityIdsStatus] = useState<EntityIdsStatus>({});
  const setCurrentEntityStatus = useCallback(
    (value: SelectOptionT[]) => {
      if (!currentEntityId) return;

      setEntityIdsStatus((curr) => ({
        ...curr,
        [currentEntityId]: value,
      }));
    },
    [currentEntityId],
  );
  const currentEntityStatus = useMemo(
    () => (currentEntityId ? entityIdsStatus[currentEntityId] || [] : []),
    [currentEntityId, entityIdsStatus],
  );

  return {
    entityStatusOptions,
    setEntityStatusOptions,
    entityIdsStatus,
    setEntityIdsStatus,
    currentEntityStatus,
    setCurrentEntityStatus,
  };
};
