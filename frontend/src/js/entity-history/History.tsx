import styled from "@emotion/styled";
import { useEffect, useMemo, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useSelector } from "react-redux";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";

import { DetailControl, DetailLevel } from "./DetailControl";
import { EntityHeader } from "./EntityHeader";
import { Navigation } from "./Navigation";
import { Timeline } from "./Timeline";
import type { EntityHistoryStateT } from "./reducer";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: ${({ theme }) => theme.col.bgAlt};
  padding: 60px 20px 20px;
  z-index: 2;
  display: grid;
  grid-template-columns: 200px 1fr;
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
`;

const Main = styled("div")`
  overflow: hidden;
  display: grid;
  gap: 10px 0;
  grid-template-areas: "header control" "timeline timeline";
  grid-template-rows: auto 1fr;
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
      <Navigation
        entityIds={entityIds}
        entityIdsStatus={entityIdsStatus}
        currentEntityId={currentEntityId}
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
    </FullScreen>
  );
};
