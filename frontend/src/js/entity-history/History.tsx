import styled from "@emotion/styled";
import { Fragment, useCallback, useEffect, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useHotkeys } from "react-hotkeys-hook";
import ReactList from "react-list";
import { useSelector } from "react-redux";

import { SelectOptionT } from "../api/types";
import { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { exists } from "../common/helpers/exists";
import { Heading3, Heading4 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

import { useUpdateHistorySession } from "./actions";
import { EntityEvent, EntityHistoryStateT } from "./reducer";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: white;
  padding: 60px 20px 20px;
  z-index: 2;
  display: grid;
  gap: 10px;
  grid-template-columns: 200px 1fr;
`;
const Navigation = styled("div")`
  display: grid;
  gap: 10px;
  overflow: hidden;
`;

const EntityIdNav = styled("div")`
  gap: 10px;
  display: grid;
  grid-template-rows: 1fr 12fr 1fr 1fr;
  overflow: hidden;
`;

const Main = styled("div")`
  overflow: hidden;
  display: grid;
  gap: 5px;
`;
const TopActions = styled("div")`
  display: flex;
`;
const Middle = styled("div")`
  height: 100%;
  overflow-y: auto;
`;
const BottomActions = styled("div")`
  display: flex;
`;

const EventTimeline = styled("div")`
  display: flex;
  align-items: center;
  height: 100%;
  overflow: hidden;
`;
const EventItemList = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  height: 100%;
  width: 100%;
  margin-left: -10px;
`;

const EventItem = styled("div")`
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 3px 0;
  position: relative;
`;

const EntityId = styled("div")<{ active?: boolean }>``;

const Bullet = styled("div")`
  width: 8px;
  height: 8px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  flex-shrink: 0;
`;

const VerticalLine = styled("div")`
  height: 100%;
  width: 2px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin: 0 5px;
`;

const Badge = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  padding: 1px 4px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.grayLight};
  font-weight: 700;
`;
const Statuses = styled("div")`
  display: flex;
  align-items: center;
  gap: 2px;
`;
const EntityStatus = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  background-color: white;
  padding: 1px 4px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;

const HeadInfo = styled("div")`
  gap: 5px;
`;
const YearHead = styled("div")`
  display: flex;
  gap: 5px;
  align-items: center;
  font-size: ${({ theme }) => theme.font.xs};
`;

const Row = styled("div")<{ active?: boolean }>`
  font-weight: 700;
  padding: 1px 3px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: ${({ active, theme }) =>
    active ? theme.col.blueGrayVeryLight : "white"};
  height: 24px;
  cursor: pointer;

  &:hover {
    background-color: ${({ active, theme }) =>
      active ? theme.col.blueGrayVeryLight : theme.col.grayVeryLight};
  }
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
`;

const SxHeading4 = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
`;
const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
`;

export const History = () => {
  const updateHistorySession = useUpdateHistorySession();
  const [entityStatusOptions, setEntityStatusOptions] = useState<
    SelectOptionT[]
  >([{ label: "Done", value: "Done" }]);
  const [entityIdsStatus, setEntityIdsStatus] = useState<{
    [id: string]: SelectOptionT[];
  }>({});

  const entityIds = useSelector<StateT, string[]>(
    (state) => state.entityHistory.entityIds,
  );

  const currentEntityId = useSelector<StateT, string | null>(
    (state) => state.entityHistory.currentEntityId,
  );
  const currentEntityData = useSelector<
    StateT,
    EntityHistoryStateT["currentEntityData"]
  >((state) => state.entityHistory.currentEntityData);

  const bucketedEntityDataByYear = currentEntityData.reduce<{
    [year: string]: EntityEvent[];
  }>((all, row) => {
    const year = row.dates.from.slice(0, 4);

    if (!all[year]) {
      all[year] = [];
    }

    all[year].push(row);

    return all;
  }, {});

  useEffect(() => {
    setEntityStatusOptions(
      [
        ...new Set([
          "done",
          ...Object.values(entityIdsStatus).flatMap((opts) =>
            opts.map((o) => o.value as string),
          ),
        ]),
      ].map((val) => ({ label: val, value: val })),
    );
  }, [entityIdsStatus]);

  const renderItem = (index: number) => {
    const entityId = entityIds[index];
    return (
      <Row
        key={entityId}
        active={entityId === currentEntityId}
        className="scrollable-list-item"
        onClick={() => updateHistorySession({ entityId })}
      >
        <EntityId>{entityId}</EntityId>
        <Statuses>
          {entityIdsStatus[entityId] &&
            entityIdsStatus[entityId].map((val) => (
              <EntityStatus key={val.value}>{val.label}</EntityStatus>
            ))}
        </Statuses>
      </Row>
    );
  };

  const goToPrev = useCallback(() => {
    const prevEntityIdx = currentEntityId
      ? Math.max(0, entityIds.indexOf(currentEntityId) - 1)
      : 0;
    updateHistorySession({
      entityId: entityIds[prevEntityIdx],
    });
  }, [currentEntityId, entityIds]);
  const goToNext = useCallback(() => {
    const nextEntityIdx = currentEntityId
      ? Math.min(entityIds.length - 1, entityIds.indexOf(currentEntityId) + 1)
      : 0;
    updateHistorySession({
      entityId: entityIds[nextEntityIdx],
    });
  }, [currentEntityId, entityIds]);

  useHotkeys("shift+up", goToPrev, [goToPrev]);
  useHotkeys("shift+down", goToNext, [goToNext]);

  return (
    <FullScreen>
      <Navigation>
        <HeadInfo>
          <SxHeading3>{entityIds.length} ids</SxHeading3>
        </HeadInfo>
        <EntityIdNav>
          <TopActions>
            <WithTooltip text="Shift+Up">
              <SxIconButton frame icon="arrow-up" onClick={goToPrev} />
            </WithTooltip>
          </TopActions>
          <Middle>
            <ReactList
              itemRenderer={renderItem}
              length={entityIds.length}
              type="uniform"
            />
          </Middle>
          <BottomActions>
            <SxWithTooltip text="Shift+Down">
              <SxIconButton frame icon="arrow-down" onClick={goToNext} />
            </SxWithTooltip>
          </BottomActions>
          <BottomActions>
            <SxIconButton
              frame
              icon="download"
              onClick={() => {
                const blob = new Blob(
                  [
                    entityIds
                      .map((id) =>
                        [
                          id,
                          entityIdsStatus[id]
                            ? entityIdsStatus[id].map((o) => o.value)
                            : "",
                        ].join(";"),
                      )
                      .join("\n"),
                  ],
                  {
                    type: "application/csv",
                  },
                );
                const url = URL.createObjectURL(blob);
                const link = document.createElement("a");
                link.href = url;
                link.download = "list.csv";
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
              }}
            />
          </BottomActions>
        </EntityIdNav>
      </Navigation>
      <Main>
        {currentEntityId && (
          <HeadInfo>
            <SxHeading3>
              {currentEntityId} – {currentEntityData.length} events
            </SxHeading3>
            <InputMultiSelect
              creatable
              onChange={(values) =>
                setEntityIdsStatus((curr) => ({
                  ...curr,
                  [currentEntityId]: values,
                }))
              }
              value={
                entityIdsStatus[currentEntityId]
                  ?.map((val) =>
                    entityStatusOptions.find((o) => o.value === val.value),
                  )
                  .filter(exists) || []
              }
              options={entityStatusOptions}
            />
          </HeadInfo>
        )}
        <ErrorBoundary
          fallbackRender={() => {
            return <div>Something went wrong here.</div>;
          }}
        >
          {Object.entries(bucketedEntityDataByYear)
            .sort(([yearA], [yearB]) => parseInt(yearB) - parseInt(yearA))
            .map(([year, events]) => (
              <Fragment key={year}>
                <YearHead key={year}>
                  <SxHeading4>{year}</SxHeading4> – {events.length} events
                </YearHead>
                <EventTimeline>
                  <VerticalLine />
                  <EventItemList>
                    {events.map((row, index) => {
                      return (
                        <EventItem key={index}>
                          <Bullet />
                          <div style={{ flexShrink: 0 }}>{row.dates.from}</div>
                          <div style={{ flexShrink: 0 }}>{row.dates.to}</div>
                          <SxWithTooltip
                            place="right"
                            html={
                              <pre
                                style={{ textAlign: "left", fontSize: "12px" }}
                              >
                                {JSON.stringify(row, null, 2)}
                              </pre>
                            }
                          >
                            <Badge
                              style={{ cursor: "pointer" }}
                              onClick={() => {
                                if (navigator.clipboard) {
                                  navigator.clipboard.writeText(
                                    JSON.stringify(row, null, 2),
                                  );
                                }
                              }}
                            >
                              DATA
                            </Badge>
                          </SxWithTooltip>
                          {Object.keys(row)
                            .slice(4)
                            .reverse()
                            .map((key, index, array) => (
                              <SxWithTooltip key={key} place="top" text={key}>
                                {row[key]}
                                {index !== array.length - 1 && " – "}
                              </SxWithTooltip>
                            ))}
                        </EventItem>
                      );
                    })}
                  </EventItemList>
                </EventTimeline>
              </Fragment>
            ))}
        </ErrorBoundary>
      </Main>
    </FullScreen>
  );
};
