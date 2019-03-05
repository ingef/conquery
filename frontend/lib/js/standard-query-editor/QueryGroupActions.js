// @flow

import React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import { Icon } from "../icon/FaIcon";

const Actions = styled("div")`
  margin: 0 0 4px;
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
`;

const activeStyle = ({ theme, active }) =>
  css`
    color: ${active ? theme.col.red : theme.col.black};
    ${Icon} {
      color: ${active ? theme.col.red : theme.col.black};
    }
  `;

const RedIconButton = styled(IconButton)`
  margin-right: 5px;
  padding: 0 3px;

  ${activeStyle};

  &:hover {
    ${activeStyle}
    opacity: 0.7;
  }
`;

type PropsType = {
  excludeActive: string,
  dateActive: string,
  onExcludeClick: () => void,
  onDeleteGroup: () => void,
  onDateClick: () => void
};

const QueryGroupActions = (props: PropsType) => {
  return (
    <Actions>
      <div>
        <RedIconButton
          active={props.excludeActive}
          large
          icon="ban"
          onClick={props.onExcludeClick}
        >
          {T.translate("queryEditor.exclude")}
        </RedIconButton>
        <StyledIconButton
          active={props.dateActive}
          large
          icon="calendar-o"
          onClick={props.onDateClick}
        >
          {T.translate("queryEditor.date")}
        </StyledIconButton>
      </div>
      <Right>
        <IconButton noFrame tiny icon="close" onClick={props.onDeleteGroup} />
      </Right>
    </Actions>
  );
};

export default QueryGroupActions;
