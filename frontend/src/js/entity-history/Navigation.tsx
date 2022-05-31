import styled from "@emotion/styled";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import ReactList from "react-list";

import { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { downloadBlob } from "../common/helpers/downloadBlob";
import { Heading3 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";

import { useUpdateHistorySession } from "./actions";

const Root = styled("div")`
  display: grid;
  gap: 10px;
  overflow: hidden;
`;

const EntityIdNav = styled("div")`
  gap: 10px;
  display: grid;
  grid-template-rows: auto 12fr auto auto;
  overflow: hidden;
`;
const TopActions = styled("div")`
  display: flex;
`;

const HeadInfo = styled("div")`
  gap: 5px;
`;
const Middle = styled("div")`
  height: 100%;
  overflow-y: auto;
  border-radius: 10px;
  box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1);
`;
const BottomActions = styled("div")`
  display: flex;
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
`;

const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
  width: 100%;
`;

const Row = styled("div")<{ active?: boolean }>`
  padding: 1px 3px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  background-color: ${({ active, theme }) =>
    active ? theme.col.blueGrayVeryLight : "white"};
  height: 24px;
  cursor: pointer;

  &:hover {
    background-color: ${({ active, theme }) =>
      active ? theme.col.blueGrayVeryLight : theme.col.grayVeryLight};
  }
`;

const Statuses = styled("div")`
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
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
const EntityId = styled("div")<{ active?: boolean }>`
  font-weight: 700;
`;
const Number = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
`;

interface Props {
  entityIds: string[];
  entityIdsStatus: { [entityId: string]: SelectOptionT[] };
  currentEntityId: string | null;
}

export const Navigation = ({
  entityIds,
  entityIdsStatus,
  currentEntityId,
}: Props) => {
  const updateHistorySession = useUpdateHistorySession();

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

  const magnitude = useMemo(
    () => Math.ceil(Math.log(entityIds.length) / Math.log(10)),
    [entityIds.length],
  );

  const renderItem = (index: number) => {
    const entityId = entityIds[index];

    return (
      <Row
        key={entityId}
        active={entityId === currentEntityId}
        className="scrollable-list-item"
        onClick={() => updateHistorySession({ entityId })}
      >
        <Number style={{ width: 20 + 8 * magnitude }}>#{index}</Number>
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

  return (
    <Root>
      <HeadInfo>
        <SxHeading3>{entityIds.length} ids</SxHeading3>
      </HeadInfo>
      <EntityIdNav>
        <TopActions>
          <SxWithTooltip text="Shift+Up">
            <SxIconButton frame icon="arrow-up" onClick={goToPrev} />
          </SxWithTooltip>
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
          <SxWithTooltip text="Download list">
            <SxIconButton
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

                const blob = new Blob([csvString], { type: "application/csv" });

                downloadBlob(blob, "list.csv");
              }}
            />
          </SxWithTooltip>
        </BottomActions>
      </EntityIdNav>
    </Root>
  );
};
