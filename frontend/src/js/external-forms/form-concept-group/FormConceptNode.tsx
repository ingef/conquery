import styled from "@emotion/styled";
import {
  faCompressArrowsAlt,
  faExpandArrowsAlt,
} from "@fortawesome/free-solid-svg-icons";
import { useRef, FC } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";

import { getWidthAndHeight } from "../../app/DndProvider";
import IconButton from "../../button/IconButton";
import { canDropConceptTreeNodeBeDropped } from "../../query-node-editor/ConceptDropzone";
import { HoverNavigatable } from "../../small-tab-navigation/HoverNavigatable";
import { getRootNodeLabel } from "../../standard-query-editor/helper";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import WithTooltip from "../../tooltip/WithTooltip";

const Root = styled("div")<{
  active?: boolean;
}>`
  padding: 5px 10px;
  cursor: pointer;
  background-color: white;
  max-width: 200px;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: background-color ${({ theme }) => theme.transitionTime};
  border: ${({ theme, active }) =>
    active
      ? `2px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayMediumLight}`};
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
  conceptNode: DragItemConceptTreeNode;
  name: string;
  onClick: () => void;
  hasNonDefaultSettings: boolean;
  hasFilterValues: boolean;
  expand?: {
    onClick: () => void;
    expandable: boolean;
    active: boolean;
  };
}

// generalized node to handle concepts queried in forms
const FormConceptNode: FC<PropsT> = ({
  valueIdx,
  conceptIdx,
  conceptNode,
  onClick,
  hasNonDefaultSettings,
  hasFilterValues,
  expand,
}) => {
  const { t } = useTranslation();
  const rootNodeLabel = getRootNodeLabel(conceptNode);
  const ref = useRef<HTMLDivElement | null>(null);

  const item: DragItemConceptTreeNode = {
    ...conceptNode,
    dragContext: {
      movedFromAndIdx: valueIdx,
      movedFromOrIdx: conceptIdx,
      width: 0,
      height: 0,
    },
  };
  const [, drag] = useDrag<DragItemConceptTreeNode, void, {}>({
    type: item.type,
    item: () => ({
      ...item,
      dragContext: {
        ...item.dragContext,
        ...getWidthAndHeight(ref),
      },
    }),
  });

  const tooltipText = hasNonDefaultSettings
    ? t("queryEditor.hasNonDefaultSettings")
    : hasFilterValues
    ? t("queryEditor.hasDefaultSettings")
    : undefined;

  return (
    <HoverNavigatable
      triggerNavigate={onClick}
      canDrop={canDropConceptTreeNodeBeDropped(conceptNode)}
      highlightDroppable
    >
      <Root
        ref={(instance) => {
          ref.current = instance;
          drag(instance);
        }}
        active={hasNonDefaultSettings || hasFilterValues}
        onClick={onClick}
      >
        <div>
          <WithTooltip text={tooltipText}>
            <>
              {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
              <Label>{conceptNode && conceptNode.label}</Label>
              {conceptNode && !!conceptNode.description && (
                <Description>{conceptNode.description}</Description>
              )}
            </>
          </WithTooltip>
        </div>
        <Right>
          {expand && expand.expandable && (
            <WithTooltip text={t("externalForms.common.concept.expand")}>
              <SxIconButton
                icon={expand.active ? faCompressArrowsAlt : faExpandArrowsAlt}
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
    </HoverNavigatable>
  );
};

export default FormConceptNode;
