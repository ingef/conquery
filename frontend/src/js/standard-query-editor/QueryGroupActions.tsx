import React, { FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import { Icon } from "../icon/FaIcon";

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

const QueryGroupActions: FC<PropsT> = ({
  excludeActive,
  dateActive,
  onExcludeClick,
  onDeleteGroup,
  onDateClick,
}) => {
  return (
    <Actions>
      <div>
        <RedIconButton
          red
          tight
          active={excludeActive}
          icon="ban"
          onClick={onExcludeClick}
        >
          {T.translate("queryEditor.exclude")}
        </RedIconButton>
        <StyledIconButton
          active={dateActive}
          regular
          tight
          icon="calendar"
          onClick={onDateClick}
        >
          {T.translate("queryEditor.date")}
        </StyledIconButton>
      </div>
      <Right>
        <IconButton tiny icon="times" onClick={onDeleteGroup} />
      </Right>
    </Actions>
  );
};

export default QueryGroupActions;
