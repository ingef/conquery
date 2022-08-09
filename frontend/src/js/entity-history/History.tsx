import styled from "@emotion/styled";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import ErrorFallback from "../error-fallback/ErrorFallback";

import ContentControl, { useContentControl } from "./ContentControl";
import { DetailControl, DetailLevel } from "./DetailControl";
import { DownloadEntityDataButton } from "./DownloadEntityDataButton";
import { EntityHeader } from "./EntityHeader";
import type { LoadingPayload } from "./LoadHistoryDropzone";
import { Navigation } from "./Navigation";
import SourcesControl from "./SourcesControl";
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
  gap: 18px;
  margin: 0 20px;
`;

const Main = styled("div")`
  overflow: hidden;
  height: 100%;
  display: grid;
  gap: 10px 0;
  grid-template-areas: "header control" "line line" "timeline timeline";
  grid-template-rows: auto auto 1fr;
  padding: 55px 0 10px;
`;

const HorizontalLine = styled("div")`
  grid-area: line;
  height: 1px;
  background-color: ${({ theme }) => theme.col.grayLight};
  width: 100%;
`;

const SxSourcesControl = styled(SourcesControl)`
  flex-shrink: 0;
  width: 450px;
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

  const { options, sourcesSet, sourcesFilter, setSourcesFilter } =
    useSourcesControl();

  const { contentFilter, setContentFilter } = useContentControl();

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

  const onLoadFromFile = useCallback(
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
    [setEntityIdsStatus, setEntityStatusOptions, updateHistorySession],
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
          onLoadFromFile={onLoadFromFile}
        />
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Main key={currentEntityId}>
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
              <SxSourcesControl
                options={options}
                sourcesFilter={sourcesFilter}
                setSourcesFilter={setSourcesFilter}
              />
              <ContentControl
                value={contentFilter}
                onChange={setContentFilter}
              />
              <DownloadEntityDataButton />
            </Controls>
            <HorizontalLine />
            <SxTimeline
              detailLevel={detailLevel}
              sources={sourcesSet}
              contentFilter={contentFilter}
            />
          </Main>
        </ErrorBoundary>
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

const useSourcesControl = () => {
  const [sourcesFilter, setSourcesFilter] = useState<SelectOptionT[]>([]);
  const uniqueSources = useSelector<StateT, string[]>(
    (state) => state.entityHistory.uniqueSources,
  );

  const defaultSources = useMemo(
    () =>
      uniqueSources.map((s) => ({
        label: s,
        value: s,
      })),
    [uniqueSources],
  );

  const sourcesSet = useMemo(
    () => new Set(sourcesFilter.map((o) => o.value as string)),
    [sourcesFilter],
  );

  useEffect(
    function takeDefaultIfEmpty() {
      setSourcesFilter((curr) => (curr.length === 0 ? defaultSources : curr));
    },
    [defaultSources],
  );

  return {
    options: defaultSources,
    sourcesSet,
    sourcesFilter,
    setSourcesFilter,
  };
};
