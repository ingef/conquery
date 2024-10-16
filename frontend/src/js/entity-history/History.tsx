import styled from "@emotion/styled";
import {useCallback, useMemo, useState} from "react";
import {ErrorBoundary} from "react-error-boundary";
import {useHotkeys} from "react-hotkeys-hook";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";

import type {EntityInfo, ResultUrlWithLabel, SelectOptionT, TimeStratifiedInfo,} from "../api/types";
import type {StateT} from "../app/reducers";
import ErrorFallback from "../error-fallback/ErrorFallback";
import DownloadResultsDropdownButton from "../query-runner/DownloadResultsDropdownButton";

import {Panel, PanelGroup} from "react-resizable-panels";
import {ResizeHandle} from "../common/ResizeHandle";
import ContentControl, {useContentControl} from "./ContentControl";
import {DetailControl, DetailLevel} from "./DetailControl";
import {EntityHeader} from "./EntityHeader";
import InteractionControl from "./InteractionControl";
import type {LoadingPayload} from "./LoadHistoryDropzone";
import {Navigation} from "./Navigation";
import SourcesControl from "./SourcesControl";
import {Timeline} from "./Timeline";
import VisibilityControl from "./VisibilityControl";
import {useUpdateHistorySession} from "./actions";
import {EntityId} from "./reducer";
import SearchControl from "./timeline-search/SearchControl";
import {TimelineSearchProvider} from "./timeline-search/timelineSearchState";
import {useEntityStatus} from "./useEntityStatus";
import {useOpenCloseInteraction} from "./useOpenCloseInteraction";
import {useSourcesControl} from "./useSourcesControl";

const FullScreen = styled("div")`
  position: fixed;
  top: 0;
  left: 0;
  height: 100%;
  width: 100%;
  z-index: 2;
  background-color: ${({theme}) => theme.col.bgAlt};
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
  border-right: 1px solid ${({theme}) => theme.col.grayLight};
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
  border-top: 1px solid ${({theme}) => theme.col.grayLight};
`;

const SxSourcesControl = styled(SourcesControl)`
  flex-shrink: 0;
  width: 450px;
`;

export interface EntityIdsStatus {
    [entityId: string]: SelectOptionT[];
}

export const History = () => {
    const {t} = useTranslation();
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
    const {updateHistorySession} = useUpdateHistorySession();

    const {options, sourcesSet, sourcesFilter, setSourcesFilter} =
        useSourcesControl();

    const {contentFilter, setContentFilter} = useContentControl();

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
    } = useEntityStatus({currentEntityId: currentEntityId?.id || null});

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

    const {getIsOpen, toggleOpenYear, toggleOpenQuarter, closeAll, openAll} =
        useOpenCloseInteraction();

    return (
        <TimelineSearchProvider>
            <FullScreen>
                <PanelGroup units="pixels" direction="horizontal">
                    <Panel minSize={400} defaultSize={400} maxSize={800}>
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
                    </Panel>
                    <ResizeHandle/>
                    <Panel minSize={500}>
                        <ErrorBoundary fallback={<ErrorFallback allowFullRefresh/>}>
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
                                        <SearchControl/>
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
                                </Flex>
                            </Main>
                        </ErrorBoundary>
                    </Panel>
                </PanelGroup>
            </FullScreen>
        </TimelineSearchProvider>
    );
};
