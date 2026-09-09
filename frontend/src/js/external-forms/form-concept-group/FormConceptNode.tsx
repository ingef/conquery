import {
  faCompressArrowsAlt,
  faExpandArrowsAlt,
} from "@fortawesome/free-solid-svg-icons";
import { useRef } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { getWidthAndHeight } from "../../app/DndProvider";
import IconButton from "../../button/IconButton";
import { canNodeBeDropped } from "../../model/node";
import { HoverNavigatable } from "../../small-tab-navigation/HoverNavigatable";
import { getRootNodeLabel } from "../../standard-query-editor/helper";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import WithTooltip from "../../ui-components/WithTooltip";

const node = tv({
  base: [
    "grid grid-cols-[1fr_auto]",
    "max-w-[200px]",
    "px-[10px] py-[5px]",
    "cursor-pointer",
    "rounded",
    "hover:bg-bg-100",
    "transition-[background-color] duration-100",
    "text-sm",
  ],
  variants: {
    active: {
      true: "border-2 border-primary-500",
      false: "border border-gray-400",
    },
  },
});

const labelText = tv({
  base: ["m-0", "[word-break:break-word]", "leading-[1.2]", "text-base"],
});

const descriptionText = tv({
  base: [
    "mt-[3px]",
    "[word-break:break-word]",
    "leading-[1.2]",
    "uppercase",
    "text-xs",
  ],
});

const rootNode = tv({
  base: [
    "mb-1",
    "leading-none",
    "uppercase",
    "font-bold",
    "text-xs",
    "text-primary-500",
    "[word-break:break-word]",
  ],
});

// generalized node to handle concepts queried in forms
const FormConceptNode = ({
  valueIdx,
  conceptIdx,
  conceptNode,
  onClick,
  hasNonDefaultSettings,
  hasFilterValues,
  expand,
  deleteFromOtherField,
  fieldName,
  rowPrefixFieldname,
}: {
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
  deleteFromOtherField: () => void;
  fieldName: string;
  rowPrefixFieldname?: string;
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
      rowPrefixFieldname: rowPrefixFieldname,
    },
  };
  const [, drag] = useDrag<DragItemConceptTreeNode, void>({
    type: item.type,
    item: () => ({
      ...item,
      dragContext: {
        ...item.dragContext,
        ...getWidthAndHeight(ref),
        deleteFromOtherField,
        movedFromFieldName: fieldName,
        rowPrefixFieldname: rowPrefixFieldname,
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
      canDrop={(item) => canNodeBeDropped(conceptNode, item)}
      highlightDroppable
    >
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
      {/* biome-ignore lint/a11y/noStaticElementInteractions: see above */}
      <div
        className={node({ active: hasNonDefaultSettings || hasFilterValues })}
        ref={(instance) => {
          ref.current = instance;
          drag(instance);
        }}
        onClick={onClick}
      >
        <div>
          <WithTooltip text={tooltipText}>
            {/* biome-ignore lint/complexity/noUselessFragments: WithTooltip takes a single child */}
            <>
              {rootNodeLabel && <p className={rootNode()}>{rootNodeLabel}</p>}
              <p className={labelText()}>{conceptNode?.label}</p>
              {conceptNode && !!conceptNode.description && (
                <div className={descriptionText()}>
                  {conceptNode.description}
                </div>
              )}
            </>
          </WithTooltip>
        </div>
        <div className="ml-[10px]">
          {expand?.expandable && (
            <WithTooltip text={t("externalForms.common.concept.expand")}>
              <IconButton
                className="px-[6px] py-0"
                icon={expand.active ? faCompressArrowsAlt : faExpandArrowsAlt}
                tiny
                onClick={(e) => {
                  e.stopPropagation();
                  expand.onClick();
                }}
              />
            </WithTooltip>
          )}
        </div>
      </div>
    </HoverNavigatable>
  );
};

export default FormConceptNode;
