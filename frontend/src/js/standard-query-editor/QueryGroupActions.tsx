import styled from "@emotion/styled";
import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { faBan, faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { Icon } from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

const Actions = styled("div")`
  margin: 0 0 6px;
  text-align: left;
  height: 18px; // To provide enough space when only --right is available
`;

const Right = styled("div")`
  position: absolute;
  right: 7px;
  top: 5px;
`;

const StyledIconButton = styled(IconButton)`
  margin-right: 5px;
  padding: 0 3px;

  text-decoration: ${({ active }) => (active ? "underline" : "initial")};
`;

const RedIconButton = styled(IconButton)`
  margin-right: 5px;
  padding: 0 3px;

  color: ${({ active, theme }) => (active ? theme.col.red : theme.col.black)};
  ${Icon} {
    color: ${({ active, theme }) => (active ? theme.col.red : theme.col.black)};
  }

  &:hover {
    opacity: 0.7;
    color: ${({ active, theme }) => (active ? theme.col.red : theme.col.black)};
    ${Icon} {
      color: ${({ active, theme }) =>
        active ? theme.col.red : theme.col.black};
    }
  }
`;

interface PropsT {
  excludeActive: boolean;
  dateActive: boolean;
  onExcludeClick: () => void;
  onDeleteGroup: () => void;
  onDateClick: () => void;
}

const QueryGroupActions = ({
  excludeActive,
  dateActive,
  onExcludeClick,
  onDeleteGroup,
  onDateClick,
}: PropsT) => {
  const { t } = useTranslation();

  return (
    <Actions>
      <div>
        <WithTooltip text={t("help.queryEditorExclude")} lazy>
          <RedIconButton
            red
            tight
            active={excludeActive}
            icon={faBan}
            onClick={onExcludeClick}
          >
            {t("queryEditor.exclude")}
          </RedIconButton>
        </WithTooltip>
        <WithTooltip text={t("help.queryEditorDate")} lazy>
          <StyledIconButton
            active={dateActive}
            tight
            icon={faCalendar}
            onClick={onDateClick}
          >
            {t("queryEditor.date")}
          </StyledIconButton>
        </WithTooltip>
      </div>
      <Right>
        <WithTooltip text={t("queryEditor.removeColumn")}>
          <IconButton tiny icon={faTimes} onClick={onDeleteGroup} />
        </WithTooltip>
      </Right>
    </Actions>
  );
};

export default memo(QueryGroupActions);
