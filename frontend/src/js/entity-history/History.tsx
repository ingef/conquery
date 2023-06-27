import styled from "@emotion/styled";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type {
  EntityInfo,
  HistorySources,
  ResultUrlWithLabel,
  SelectOptionT,
  TimeStratifiedInfo,
} from "../api/types";
import type { StateT } from "../app/reducers";
import ErrorFallback from "../error-fallback/ErrorFallback";
import DownloadResultsDropdownButton from "../query-runner/DownloadResultsDropdownButton";

import ContentControl, { useContentControl } from "./ContentControl";
import { DetailControl, DetailLevel } from "./DetailControl";
import { EntityHeader } from "./EntityHeader";
import InteractionControl from "./InteractionControl";
import type { LoadingPayload } from "./LoadHistoryDropzone";
import { Navigation } from "./Navigation";
import SourcesControl from "./SourcesControl";
import Timeline from "./Timeline";
import VisibilityControl from "./VisibilityControl";
import { useUpdateHistorySession } from "./actions";
import { EntityId } from "./reducer";

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
  padding: 10px 0 0;
  border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  display: flex;
  flex-direction: column;
  gap: 20px;
`;
const SidebarBottom = styled("div")`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  justify-content: flex-end;
`;

const Header = styled("div")`
  display: flex;
  flex-direction: row-reverse;
  gap: 15px;
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
  const { t } = useTranslation();
  const entityIds = useSelector<StateT, EntityId[]>(
    (state) => state.entityHistory.entityIds,
  );
  const currentEntityId = useSelector<StateT, EntityId | null>(
    (state) => state.entityHistory.currentEntityId,
  );
  const currentEntityInfos = useSelector<StateT, EntityInfo[]>(
    (state) => state.entityHistory.currentEntityInfos,
  );
  const currentEntityTimeStratifiedInfos = useSelector<
    StateT,
    TimeStratifiedInfo[]
  >((state) => state.entityHistory.currentEntityTimeStratifiedInfos);
  const resultUrls = useSelector<StateT, ResultUrlWithLabel[]>(
    (state) => state.entityHistory.resultUrls,
  );

  const [blurred, setBlurred] = useState(false);
  const toggleBlurred = useCallback(() => setBlurred((v) => !v), []);
  useHotkeys("p", toggleBlurred, [toggleBlurred]);

  const [showAdvancedControls, setShowAdvancedControls] = useState(false);

  useHotkeys("shift+alt+h", () => {
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
  } = useEntityStatus({ currentEntityId: currentEntityId?.id || null });

  const onResetEntityStatus = useCallback(() => {
    setEntityIdsStatus({});
  }, [setEntityIdsStatus]);

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

  const { getIsOpen, toggleOpenYear, toggleOpenQuarter, closeAll, openAll } =
    useOpenCloseInteraction();

  return (
    <FullScreen>
      {/*
          react-split-pane is not compatible with react 18 types,
          TODO: Move to https://github.com/johnwalley/allotment
          @ts-ignore */}
      <SplitPane
        split="vertical"
        minSize={400}
        maxSize={-500}
        defaultSize="400px"
      >
        <SxNavigation
          blurred={blurred}
          entityIds={entityIds}
          entityIdsStatus={entityIdsStatus}
          currentEntityId={currentEntityId}
          currentEntityIndex={currentEntityIndex}
          entityStatusOptions={entityStatusOptions}
          setEntityStatusOptions={setEntityStatusOptions}
          onLoadFromFile={onLoadFromFile}
          onResetHistory={onResetEntityStatus}
        />
        <ErrorBoundary fallback={<ErrorFallback allowFullRefresh />}>
          <Main>
            <Header>
              <Controls>
                <SxSourcesControl
                  options={options}
                  sourcesFilter={sourcesFilter}
                  setSourcesFilter={setSourcesFilter}
                />
              </Controls>
              {currentEntityId && (
                <EntityHeader
                  blurred={blurred}
                  currentEntityIndex={currentEntityIndex}
                  currentEntityId={currentEntityId}
                  status={currentEntityStatus}
                  setStatus={setCurrentEntityStatus}
                  entityStatusOptions={entityStatusOptions}
                />
              )}
            </Header>
            <Flex>
              <Sidebar>
                <VisibilityControl
                  blurred={blurred}
                  toggleBlurred={toggleBlurred}
                />
                {showAdvancedControls && (
                  <DetailControl
                    detailLevel={detailLevel}
                    setDetailLevel={setDetailLevel}
                  />
                )}
                <InteractionControl onCloseAll={closeAll} onOpenAll={openAll} />
                <ContentControl
                  value={contentFilter}
                  onChange={setContentFilter}
                />
                <SidebarBottom>
                  {resultUrls.length > 0 && (
                    <DownloadResultsDropdownButton
                      tiny
                      resultUrls={resultUrls}
                      tooltip={t("history.downloadEntityData")}
                    />
                  )}
                </SidebarBottom>
              </Sidebar>
              <SxTimeline
                blurred={blurred}
                detailLevel={detailLevel}
                sources={sourcesSet}
                contentFilter={contentFilter}
                currentEntityInfos={currentEntityInfos}
                currentEntityTimeStratifiedInfos={
                  currentEntityTimeStratifiedInfos
                }
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

export const useDefaultStatusOptions = () => {
  const { t } = useTranslation();

  return useMemo(
    () => [
      {
        label: t("history.options.check"),
        value: t("history.options.check") as string,
      },
      {
        label: t("history.options.noCheck"),
        value: t("history.options.noCheck") as string,
      },
    ],
    [t],
  );
};

const useEntityStatus = ({
  currentEntityId,
}: {
  currentEntityId: string | null;
}) => {
  const defaultStatusOptions = useDefaultStatusOptions();
  const [entityStatusOptions, setEntityStatusOptions] =
    useState<SelectOptionT[]>(defaultStatusOptions);

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

  const sources = useSelector<StateT, HistorySources>(
    (state) => state.entityHistory.defaultParams.sources,
  );
  const allSourcesOptions = useMemo(
    () =>
      sources.all.map((s) => ({
        label: s.label,
        value: s.label, // Gotta use label since the value in the entity CSV is the source label as well
      })),
    [sources.all],
  );
  const defaultSourcesOptions = useMemo(
    () =>
      sources.default.map((s) => ({
        label: s.label,
        value: s.label, // Gotta use label since the value in the entity CSV is the source label as well
      })),
    [sources.default],
  );

  // TODO: Figure out whether we still need the current entity unique sources
  //
  // const currentEntityUniqueSources = useSelector<StateT, string[]>(
  //   (state) => state.entityHistory.currentEntityUniqueSources,
  // );
  // const currentEntitySourcesOptions = useMemo(
  //   () =>
  //     currentEntityUniqueSources.map((s) => ({
  //       label: s,
  //       value: s,
  //     })),
  //   [currentEntityUniqueSources],
  // );

  const sourcesSet = useMemo(
    () => new Set(sourcesFilter.map((o) => o.value as string)),
    [sourcesFilter],
  );

  useEffect(
    function takeDefaultIfEmpty() {
      setSourcesFilter((curr) =>
        curr.length === 0 ? defaultSourcesOptions : curr,
      );
    },
    [defaultSourcesOptions],
  );

  return {
    options: allSourcesOptions,
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

  const closeAll = useCallback(() => {
    setIsOpen({});
  }, []);

  const openAll = useCallback(() => {
    const lastYearsToUse = 20;
    const currYear = new Date().getFullYear();
    const years = [...Array(lastYearsToUse).keys()].map((i) => currYear - i);

    const newIsOpen: Record<string, boolean> = {};

    for (const year of years) {
      for (const quarter of [1, 2, 3, 4]) {
        newIsOpen[toId(year, quarter)] = true;
      }
    }

    setIsOpen(newIsOpen);
  }, [toId]);

  return {
    getIsOpen,
    toggleOpenYear,
    toggleOpenQuarter,
    closeAll,
    openAll,
  };
};
