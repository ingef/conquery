import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { BadgeToggleButton } from "../button/BadgeToggleButton";
import { Heading3 } from "../headings/Headings";

import { EntityId } from "./reducer";

const Root = styled("div")`
  display: flex;
  align-items: center;
  gap: 30px;
  padding-left: 10px;
  justify-content: space-between;
  width: 100%;
`;
const Flex = styled("div")`
  display: flex;
  align-items: center;
  gap: 30px;
`;
const Buttons = styled("div")`
  display: grid;
  grid-template-rows: 1fr 1fr;
  grid-auto-flow: column;
  gap: 5px;
`;

const SxHeading3 = styled(Heading3)<{ blurred?: boolean }>`
  flex-shrink: 0;
  margin: 0;
  ${({ blurred }) => blurred && "filter: blur(6px);"}
`;
const Subtitle = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  margin-top: 5px;
`;
const EntityBadge = styled("div")`
  display: flex;
  gap: 5px;
`;
const Avatar = styled(SxHeading3)`
  color: ${({ theme }) => theme.col.gray};
  font-weight: 300;
`;

export const EntityHeader = ({
  blurred,
  className,
  currentEntityIndex,
  currentEntityId,
  status,
  setStatus,
  entityStatusOptions,
}: {
  blurred?: boolean;
  className?: string;
  currentEntityIndex: number;
  currentEntityId: EntityId;
  status: SelectOptionT[];
  setStatus: (value: SelectOptionT[]) => void;
  entityStatusOptions: SelectOptionT[];
}) => {
  const totalEvents = useSelector<StateT, number>(
    (state) => state.entityHistory.currentEntityData.length,
  );

  const { t } = useTranslation();

  const toggleOption = (option: SelectOptionT) => () => {
    const newStatus = [...status];
    const index = newStatus.findIndex((val) => val.value === option.value);
    if (index === -1) {
      newStatus.push(option);
    } else {
      newStatus.splice(index, 1);
    }
    setStatus(newStatus);
  };

  return (
    <Root className={className}>
      <Flex>
        <div>
          <EntityBadge>
            <Avatar>#{currentEntityIndex + 1}</Avatar>
            <SxHeading3 blurred={blurred}>{currentEntityId.id}</SxHeading3>
          </EntityBadge>
          <Subtitle>
            {totalEvents} {t("history.events", { count: totalEvents })}
          </Subtitle>
        </div>
      </Flex>
      <Buttons>
        {entityStatusOptions.map((option, i) => (
          <span key={option.label + i}>
            <BadgeToggleButton
              active={!!status.find((opt) => opt.value === option.value)}
              onClick={toggleOption(option)}
              hotkey={i < 9 ? String(i + 1) : undefined}
            >
              {option.label}
            </BadgeToggleButton>
          </span>
        ))}
      </Buttons>
    </Root>
  );
};
