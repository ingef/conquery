import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { SelectOptionT } from "../api/types";
import { BadgeToggleButton } from "../button/BadgeToggleButton";
import { Heading3 } from "../headings/Headings";

const Root = styled("div")`
  display: flex;
  align-items: center;
  gap: 15px;
  padding-left: 10px;
`;
const Flex = styled("div")`
  display: flex;
  align-items: center;
  gap: 5px;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
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

interface Props {
  className?: string;
  currentEntityIndex: number;
  currentEntityId: string;
  totalEvents: number;
  status: SelectOptionT[];
  setStatus: (value: SelectOptionT[]) => void;
  entityStatusOptions: SelectOptionT[];
}

export const EntityHeader = ({
  className,
  currentEntityIndex,
  currentEntityId,
  totalEvents,
  status,
  setStatus,
  entityStatusOptions,
}: Props) => {
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
      <div>
        <EntityBadge>
          <Avatar>#{currentEntityIndex + 1}</Avatar>
          <SxHeading3>{currentEntityId}</SxHeading3>
        </EntityBadge>
        <Subtitle>
          {totalEvents} {t("history.events", { count: totalEvents })}
        </Subtitle>
      </div>
      <Flex>
        {entityStatusOptions.map((option, i) => (
          <BadgeToggleButton
            active={!!status.find((opt) => opt.value === option.value)}
            onClick={toggleOption(option)}
            hotkey={i < 9 ? String(i + 1) : undefined}
          >
            {option.label}
          </BadgeToggleButton>
        ))}
      </Flex>
    </Root>
  );
};
