import styled from "@emotion/styled";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useHotkeys } from "react-hotkeys-hook";
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

const Controls = styled("div")`
  display: flex;
  align-items: center;
  gap: 18px;
  margin-right: 20px;
`;

const Sidebar = styled("div")`
  padding: 10px 0;
  border-right: 1px solid ${({ theme }) => theme.col.grayLight};
`;

const Header = styled("div")`
  display: flex;
  justify-content: space-between;
`;

const Main = styled("div")`
  overflow: hidden;
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  padding: 55px 0 10px;
  gap: 10px;
`;

const Flex = styled("div")`
  display: flex;
  height: 100%;
  overflow: hidden;
  border-top: 1px solid ${({ theme }) => theme.col.grayLight};
`;

const SxSourcesControl = styled(SourcesControl)`
  flex-shrink: 0;
  width: 450px;
`;

const SxTimeline = styled(Timeline)`
  margin: 10px 0 0;
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

  const [showAdvancedControls, setShowAdvancedControls] = useState(false);

  useHotkeys("shift+option+h", () => {
    setShowAdvancedControls((v) => !v);
  });

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

  const { getIsOpen, toggleOpenYear, toggleOpenQuarter } =
    useOpenCloseInteraction();

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
          <Main>
            <Header>
              {currentEntityId && (
                <EntityHeader
                  currentEntityIndex={currentEntityIndex}
                  currentEntityId={currentEntityId}
                  status={currentEntityStatus}
                  setStatus={setCurrentEntityStatus}
                  entityStatusOptions={entityStatusOptions}
                />
              )}
              <Controls>
                <SxSourcesControl
                  options={options}
                  sourcesFilter={sourcesFilter}
                  setSourcesFilter={setSourcesFilter}
                />
                <DownloadEntityDataButton />
              </Controls>
            </Header>
            <Flex>
              <Sidebar>
                {showAdvancedControls && (
                  <DetailControl
                    detailLevel={detailLevel}
                    setDetailLevel={setDetailLevel}
                  />
                )}

                <ContentControl
                  value={contentFilter}
                  onChange={setContentFilter}
                />
              </Sidebar>
              <SxTimeline
                detailLevel={detailLevel}
                sources={sourcesSet}
                contentFilter={contentFilter}
                getIsOpen={getIsOpen}
                toggleOpenYear={toggleOpenYear}
                toggleOpenQuarter={toggleOpenQuarter}
              />
            </Flex>
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

const useOpenCloseInteraction = () => {
  const [isOpen, setIsOpen] = useState<Record<string, boolean>>({});
  const isOpenRef = useRef(isOpen);
  isOpenRef.current = isOpen;

  const toId = useCallback(
    (year: number, quarter?: number) => `${year}-${quarter}`,
    [],
  );

  const getIsOpen = useCallback(
    (year: number, quarter?: number) => {
      if (quarter) {
        return isOpen[toId(year, quarter)];
      } else {
        return [1, 2, 3, 4].every((q) => isOpen[toId(year, q)]);
      }
    },
    [isOpen, toId],
  );

  const toggleOpenYear = useCallback(
    (year: number) => {
      const quarters = [1, 2, 3, 4].map((quarter) => toId(year, quarter));
      const wasOpen = quarters.some((quarter) => isOpenRef.current[quarter]);

      setIsOpen((prev) => ({
        ...prev,
        ...Object.fromEntries(quarters.map((quarter) => [quarter, !wasOpen])),
      }));
    },
    [toId],
  );

  const toggleOpenQuarter = useCallback(
    (year: number, quarter: number) => {
      const id = toId(year, quarter);

      setIsOpen((prev) => ({ ...prev, [id]: !prev[id] }));
    },
    [toId],
  );

  return {
    getIsOpen,
    toggleOpenYear,
    toggleOpenQuarter,
  };
};
