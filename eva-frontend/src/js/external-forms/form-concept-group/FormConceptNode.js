// @flow

import React from "react";
import styled from "@emotion/styled";

import { T } from "conquery/lib/js/localization";
import IconButton from "conquery/lib/js/button/IconButton";
import WithTooltip from "conquery/lib/js/tooltip/WithTooltip";

const Root = styled("div")`
  padding: 0 10px;
  cursor: pointer;
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: ${({ active }) => (active ? "2px" : "1px")} solid
    ${({ theme, active }) =>
      active ? theme.col.blueGrayDark : theme.col.grayLight};

  &:hover {
    border: ${({ active }) => (active ? "2px" : "1px")} solid
      ${({ theme }) => theme.col.blueGrayDark};
  }

  font-size: ${({ theme }) => theme.font.sm};
`;

type PropsType = {
  valueIdx: number,
  conceptIdx: number,
  conceptNode: Object,
  name: string,
  onFilterClick: Function,
  hasActiveFilters: boolean,
  expand?: {
    onClick: Function,
    expandable: boolean,
    active: boolean
  }
};

// TODO: Refactor, add a TooltipButton in conquery and use that.

// generalized node to handle concepts queried in forms
const FormConceptNode = (props: PropsType) => {
  return (
    <Root active={props.hasActiveFilters} onClick={props.onFilterClick}>
      {props.conceptNode && props.conceptNode.label}
      {props.expand && props.expand.expandable && (
        <WithTooltip text={T.translate("externalForms.common.concept.expand")}>
          <IconButton
            icon="expand-arrows-alt"
            tiny
            onClick={e => {
              e.stopPropagation();
              props.expand.onClick();
            }}
          />
        </WithTooltip>
      )}
    </Root>
  );
};

export default FormConceptNode;
