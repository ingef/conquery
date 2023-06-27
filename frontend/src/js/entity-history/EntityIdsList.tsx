import styled from "@emotion/styled";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
import ReactList from "react-list";

import FaIcon from "../icon/FaIcon";

import type { EntityIdsStatus } from "./History";
import { useUpdateHistorySession } from "./actions";
import { EntityId } from "./reducer";

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
const TheEntityId = styled("div")<{ active?: boolean }>`
  font-weight: 700;
  flex-shrink: 0;
`;

const Number = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  flex-shrink: 0;
`;

const Gray = styled("span")`
  font-weight: 300;
  color: ${({ theme }) => theme.col.gray};
`;

const SxFaIcon = styled(FaIcon)`
  margin: 3px 6px;
`;

const Blurred = styled("span")<{ blurred?: boolean }>`
  ${({ blurred }) => blurred && "filter: blur(6px);"}
`;

export const EntityIdsList = ({
  blurred,
  currentEntityId,
  entityIds,
  entityIdsStatus,
  updateHistorySession,
  loadingId,
}: {
  blurred?: boolean;
  currentEntityId: EntityId | null;
  entityIds: EntityId[];
  updateHistorySession: ReturnType<
    typeof useUpdateHistorySession
  >["updateHistorySession"];
  entityIdsStatus: EntityIdsStatus;
  loadingId?: string;
}) => {
  const numberWidth = useMemo(() => {
    const magnitude = Math.ceil(Math.log(entityIds.length) / Math.log(10));

    return 15 + 6 * magnitude;
  }, [entityIds.length]);

  const renderItem = (index: number) => {
    const entityId = entityIds[index];

    return (
      <Row
        key={entityId.id}
        active={entityId.id === currentEntityId?.id}
        className="scrollable-list-item"
        onClick={() => updateHistorySession({ entityId, years: [] })}
      >
        <Number style={{ width: numberWidth }}>#{index + 1}</Number>
        <TheEntityId>
          <Blurred blurred={blurred}>{entityId.id}</Blurred>{" "}
          <Gray>({entityId.kind})</Gray>
        </TheEntityId>
        {loadingId === entityId.id && <SxFaIcon icon={faSpinner} />}
        <Statuses>
          {entityIdsStatus[entityId.id] &&
            entityIdsStatus[entityId.id].map((val) => (
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
