import React, { FC } from "react";
import { useDrag } from "react-dnd";
import styled from "@emotion/styled";

import { T } from "../../localization";
import IconButton from "../../button/IconButton";
import WithTooltip from "../../tooltip/WithTooltip";

import { getRootNodeLabel } from "../../standard-query-editor/helper";
import * as dndTypes from "../../common/constants/dndTypes";

const Root = styled("div")<{ active?: boolean }>`
  padding: 5px 10px;
  cursor: pointer;
  background-color: white;
  max-width: 200px;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: ${({ active }) => (active ? "2px" : "1px")} solid
    ${({ theme, active }) =>
      active ? theme.col.blueGrayDark : theme.col.grayLight};

  &:hover {
    border: ${({ active }) => (active ? "2px" : "1px")} solid
      ${({ theme }) => theme.col.blueGrayDark};
  }

  display: flex;
  align-items: center;

  font-size: ${({ theme }) => theme.font.sm};
`;

const Label = styled("p")`
  margin: 0;
  word-break: break-word;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.md};
`;
const Description = styled("div")`
  margin: 3px 0 0;
  word-break: break-word;
  line-height: 1.2;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
`;

const Left = styled("div")`
  flex-grow: 1;
  flex-basis: 0;
`;

const Right = styled("div")`
  flex-shrink: 0;
  margin-left: 10px;
`;

const RootNode = styled("p")`
  margin: 0 0 4px;
  line-height: 1;
  text-transform: uppercase;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  word-break: break-word;
`;

interface PropsT {
  valueIdx: number;
  conceptIdx: number;
  conceptNode: Object;
  name: string;
  onFilterClick: Function;
  hasActiveFilters: boolean;
  expand?: {
    onClick: Function;
    expandable: boolean;
    active: boolean;
  };
}

// TODO: Refactor, add a TooltipButton in conquery and use that.

// generalized node to handle concepts queried in forms
const FormConceptNode: FC<PropsT> = ({
  conceptNode,
  onFilterClick,
  hasActiveFilters,
  expand,
}) => {
  const rootNodeLabel = getRootNodeLabel(conceptNode);

  const [, drag] = useDrag({
    item: {
      type: dndTypes.FORM_CONCEPT_NODE,
    },
    begin: () => ({
      conceptNode,
    }),
  });

  return (
    <Root ref={drag} active={hasActiveFilters} onClick={onFilterClick}>
      <Left>
        {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
        <Label>{conceptNode && conceptNode.label}</Label>
        {conceptNode && !!conceptNode.description && (
          <Description>{conceptNode.description}</Description>
        )}
      </Left>
      <Right>
        {expand && expand.expandable && (
          <WithTooltip
            text={T.translate("externalForms.common.concept.expand")}
          >
            <IconButton
              icon={expand.active ? "compress-arrows-alt" : "expand-arrows-alt"}
              tiny
              onClick={(e) => {
                e.stopPropagation();
                expand.onClick();
              }}
            />
          </WithTooltip>
        )}
      </Right>
    </Root>
  );
};

export default FormConceptNode;
