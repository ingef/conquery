import { useCallback, useMemo, useState } from "react";
import { Toolbar } from "react-aria-components";
import { ErrorBoundary } from "react-error-boundary";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { Group, Panel } from "react-resizable-panels";
import { tv } from "tailwind-variants";
import type {
  EntityInfo,
  ResultUrlWithLabel,
  SelectOptionT,
  TimeStratifiedInfo,
} from "../api/types";
import type { StateT } from "../app/reducers";
import { ResizeHandle } from "../common/ResizeHandle";
import ErrorFallback from "../error-fallback/ErrorFallback";
import DownloadResultsDropdownButton from "../query-runner/DownloadResultsDropdownButton";
import { useUpdateHistorySession } from "./actions";
import ContentControl, { useContentControl } from "./ContentControl";
import { DetailControl, type DetailLevel } from "./DetailControl";
import { EntityHeader } from "./EntityHeader";
import InteractionControl from "./InteractionControl";
import type { LoadingPayload } from "./LoadHistoryDropzone";
import { Navigation } from "./Navigation";
import type { EntityId } from "./reducer";
import SourcesControl from "./SourcesControl";
import { Timeline } from "./Timeline";
import SearchControl from "./timeline-search/SearchControl";
import { TimelineSearchProvider } from "./timeline-search/timelineSearchState";
import { useEntityStatus } from "./useEntityStatus";
import { useOpenCloseInteraction } from "./useOpenCloseInteraction";
import { useSourcesControl } from "./useSourcesControl";
import VisibilityControl from "./VisibilityControl";

const fullScreen = tv({
  base: ["fixed top-0 left-0", "z-2", "h-full w-full", "bg-bg-100"],
});

const controls = tv({
  base: ["flex items-center", "gap-[18px]", "mr-5"],
});

const sidebar = tv({
  base: ["flex flex-col", "gap-5", "pt-[10px]", "border-r border-gray-100"],
});

const sidebarBottom = tv({
  base: ["flex flex-col justify-end", "grow", "gap-5"],
});

const header = tv({
  base: ["flex flex-row-reverse justify-between", "gap-[15px]"],
});

const main = tv({
  base: [
    "grid grid-rows-[auto_1fr]",
    "gap-[10px]",
    "h-full",
    "overflow-hidden",
    "pt-[55px] pb-[10px]",
  ],
});

const flex = tv({
  base: ["flex", "h-full", "overflow-hidden", "border-t border-gray-100"],
});

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
  const { updateHistorySession } = useUpdateHistorySession();

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
    <TimelineSearchProvider>
      <div className={fullScreen()}>
        <Group orientation="horizontal">
          <Panel minSize={400} defaultSize={400} maxSize={800}>
            <Navigation
              className="h-full pt-[55px] pb-[10px]"
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
          </Panel>
          <ResizeHandle />
          <Panel minSize={500}>
            <ErrorBoundary fallback={<ErrorFallback allowFullRefresh />}>
              <div className={main()}>
                <div className={header()}>
                  <div className={controls()}>
                    <SourcesControl
                      className="w-[450px] shrink-0"
                      options={options}
                      sourcesFilter={sourcesFilter}
                      setSourcesFilter={setSourcesFilter}
                    />
                  </div>
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
                </div>
                <div className={flex()}>
                  <Toolbar
                    orientation="vertical"
                    aria-label={t("history.toolbar")}
                    className={sidebar()}
                  >
                    <SearchControl />
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
                    <InteractionControl
                      onCloseAll={closeAll}
                      onOpenAll={openAll}
                    />
                    <ContentControl
                      value={contentFilter}
                      onChange={setContentFilter}
                    />
                    <div className={sidebarBottom()}>
                      {resultUrls.length > 0 && (
                        <DownloadResultsDropdownButton
                          tiny
                          resultUrls={resultUrls}
                          tooltip={t("history.downloadEntityData")}
                        />
                      )}
                    </div>
                  </Toolbar>
                  <Timeline
                    className="mt-[10px]"
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
                </div>
              </div>
            </ErrorBoundary>
          </Panel>
        </Group>
      </div>
    </TimelineSearchProvider>
  );
};
