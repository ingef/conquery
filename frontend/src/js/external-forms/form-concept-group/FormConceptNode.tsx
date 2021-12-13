import styled from "@emotion/styled";
import { useRef, FC } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";

import { getWidthAndHeight } from "../../app/DndProvider";
import IconButton from "../../button/IconButton";
import { FORM_CONCEPT_NODE } from "../../common/constants/dndTypes";
import { getRootNodeLabel } from "../../standard-query-editor/helper";
import type { ConceptQueryNodeType } from "../../standard-query-editor/types";
import WithTooltip from "../../tooltip/WithTooltip";

export interface DragItemFormConceptNode {
  type: "FORM_CONCEPT_NODE";
  width: number;
  height: number;
  conceptNode: ConceptQueryNodeType;
}

const Root = styled("div")<{
  hasNonDefaultSettings?: boolean;
  isEmpty?: boolean;
}>`
  padding: 5px 10px;
  cursor: pointer;
  background-color: white;
  max-width: 200px;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: background-color ${({ theme }) => theme.transitionTime};
  border: ${({ theme, hasNonDefaultSettings, isEmpty }) =>
    isEmpty
      ? `1px solid ${theme.col.grayMediumLight}`
      : hasNonDefaultSettings
      ? `2px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.orange}`};
  &:hover {
    background-color: ${({ theme }) => theme.col.bgAlt};
  }

  display: grid;
  grid-template-columns: 1fr auto;

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

const Right = styled("div")`
  margin-left: 10px;
`;

const SxIconButton = styled(IconButton)`
  padding: 0 6px;
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
  conceptNode: ConceptQueryNodeType;
  name: string;
  onFilterClick: () => void;
  hasNonDefaultSettings: boolean;
  isEmpty?: boolean;
  expand?: {
    onClick: () => void;
    expandable: boolean;
    active: boolean;
  };
}

// TODO: Refactor, add a TooltipButton in conquery and use that.

// generalized node to handle concepts queried in forms
const FormConceptNode: FC<PropsT> = ({
  conceptNode,
  onFilterClick,
  hasNonDefaultSettings,
  isEmpty,
  expand,
}) => {
  const { t } = useTranslation();
  const rootNodeLabel = getRootNodeLabel(conceptNode);
  const ref = useRef<HTMLDivElement | null>(null);

  const item: DragItemFormConceptNode = {
    type: FORM_CONCEPT_NODE,
    width: 0,
    height: 0,
    conceptNode,
  };
  const [, drag] = useDrag<DragItemFormConceptNode, void, {}>({
    item,
    begin: () => {
      return {
        ...item,
        ...getWidthAndHeight(ref),
      };
    },
  });

  const tooltipText = isEmpty
    ? t("queryEditor.hasEmptySettings")
    : hasNonDefaultSettings
    ? t("queryEditor.hasNonDefaultSettings")
    : t("queryEditor.hasDefaultSettings");

  return (
    <Root
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
      hasNonDefaultSettings={hasNonDefaultSettings}
      isEmpty={isEmpty}
      onClick={onFilterClick}
    >
      <WithTooltip text={tooltipText}>
        {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
        <Label>{conceptNode && conceptNode.label}</Label>
        {conceptNode && !!conceptNode.description && (
          <Description>{conceptNode.description}</Description>
        )}
      </WithTooltip>
      <Right>
        {expand && expand.expandable && (
          <WithTooltip text={t("externalForms.common.concept.expand")}>
            <SxIconButton
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
