// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";

type PropsType = {
  hasActiveFilters?: boolean,
  isExpandable?: boolean,
  hasDetails?: boolean,
  previousQueryLoading?: boolean,
  error?: string,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExpandClick: Function
};

const StyledIconButton = styled(IconButton)`
  padding: 0;
  margin-right: 5px;
`;

const QueryNodeActions = (props: PropsType) => {
  const base = "query-node-actions";

  return (
    <div className={`${base}`}>
      {!props.error && (
        <div className={`${base}--left`}>
          {!props.previousQueryLoading && (
            <StyledIconButton
              noFrame
              large
              icon="sliders"
              onClick={props.onFilterClick}
              active={props.hasActiveFilters}
            >
              {T.translate("queryEditor.filter")}
            </StyledIconButton>
          )}
          {!!props.previousQueryLoading && (
            <StyledIconButton noFrame large icon="spinner">
              {" " + T.translate("queryEditor.loadingPreviousQuery")}
            </StyledIconButton>
          )}
          {props.isExpandable && !props.previousQueryLoading && (
            <StyledIconButton
              noFrame
              large
              icon="expand"
              onClick={props.onExpandClick}
            >
              {T.translate("queryEditor.expand")}
            </StyledIconButton>
          )}
        </div>
      )}
      <div className={`${base}--right`}>
        <IconButton noFrame tiny icon="close" onClick={props.onDeleteNode} />
      </div>
    </div>
  );
};

export default QueryNodeActions;
