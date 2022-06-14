import styled from "@emotion/styled";
import { useMemo } from "react";
import ReactList from "react-list";

import type { EntityIdsStatus } from "./History";
import { useUpdateHistorySession } from "./actions";

const Row = styled("div")<{ active?: boolean }>`
  padding: 1px 3px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  background-color: ${({ active, theme }) =>
    active ? theme.col.blueGrayVeryLight : "white"};
  height: 24px;
  cursor: pointer;
  gap: 3px;

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
  flex-shrink: 0;
`;

interface Props {
  currentEntityId: string | null;
  entityIds: string[];
  updateHistorySession: ReturnType<typeof useUpdateHistorySession>;
  entityIdsStatus: EntityIdsStatus;
}

export const EntityIdsList = ({
  currentEntityId,
  entityIds,
  entityIdsStatus,
  updateHistorySession,
}: Props) => {
  const numberWidth = useMemo(() => {
    const magnitude = Math.ceil(Math.log(entityIds.length) / Math.log(10));

    return 15 + 6 * magnitude;
  }, [entityIds.length]);

  const renderItem = (index: number) => {
    const entityId = entityIds[index];

    return (
      <Row
        key={entityId}
        active={entityId === currentEntityId}
        className="scrollable-list-item"
        onClick={() => updateHistorySession({ entityId, years: [] })}
      >
        <Number style={{ width: numberWidth }}>#{index + 1}</Number>
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
    <ReactList
      itemRenderer={renderItem}
      length={entityIds.length}
      type="uniform"
    />
  );
};
