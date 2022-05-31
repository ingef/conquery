import styled from "@emotion/styled";
import { useEffect, useMemo, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";

import { DetailControl, DetailLevel } from "./DetailControl";
import { EntityHeader } from "./EntityHeader";
import { Navigation } from "./Navigation";
import { Timeline } from "./Timeline";
import type { EntityHistoryStateT } from "./reducer";

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
  padding: 55px 20px 15px;
`;

const SxEntityHeader = styled(EntityHeader)`
  grid-area: header;
`;
const SxTimeline = styled(Timeline)`
  grid-area: timeline;
`;
const SxDetailControl = styled(DetailControl)`
  grid-area: control;
  justify-self: end;
  margin: 0 20px;
`;

const Main = styled("div")`
  overflow: hidden;
  height: 100%;
  display: grid;
  gap: 10px 0;
  grid-template-areas: "header control" "timeline timeline";
  grid-template-rows: auto 1fr;
  padding: 55px 0 15px;
`;

export const History = () => {
  const [entityStatusOptions, setEntityStatusOptions] = useState<
    SelectOptionT[]
  >([]);
  const [entityIdsStatus, setEntityIdsStatus] = useState<{
    [id: string]: SelectOptionT[];
  }>({});

  const entityIds = useSelector<StateT, string[]>(
    (state) => state.entityHistory.entityIds,
  );

  const currentEntityId = useSelector<StateT, string | null>(
    (state) => state.entityHistory.currentEntityId,
  );
  const currentEntityIndex = useMemo(() => {
    return currentEntityId ? entityIds.indexOf(currentEntityId) : 0;
  }, [currentEntityId, entityIds]);
  const currentEntityData = useSelector<
    StateT,
    EntityHistoryStateT["currentEntityData"]
  >((state) => state.entityHistory.currentEntityData);

  const [detailLevel, setDetailLevel] = useState<DetailLevel>("summary");

  useEffect(() => {
    setEntityStatusOptions(
      [
        ...new Set(
          Object.values(entityIdsStatus).flatMap((opts) =>
            opts.map((o) => o.value as string),
          ),
        ),
      ].map((val) => ({ label: val, value: val })),
    );
  }, [entityIdsStatus]);

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
        />
        <Main>
          {currentEntityId && (
            <SxEntityHeader
              currentEntityIndex={currentEntityIndex}
              currentEntityId={currentEntityId}
              totalEvents={currentEntityData.length}
              entityIdsStatus={entityIdsStatus}
              entityStatusOptions={entityStatusOptions}
              setEntityIdsStatus={setEntityIdsStatus}
            />
          )}
          <SxDetailControl
            detailLevel={detailLevel}
            setDetailLevel={setDetailLevel}
          />
          <ErrorBoundary
            fallbackRender={() => {
              return <div>Something went wrong here.</div>;
            }}
          >
            <SxTimeline data={currentEntityData} detailLevel={detailLevel} />
          </ErrorBoundary>
        </Main>
      </SplitPane>
    </FullScreen>
  );
};
