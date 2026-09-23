import { Maximize2Icon, Minimize2Icon } from "lucide-react";
import { useRef } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { getWidthAndHeight } from "../../app/DndProvider";
import { canNodeBeDropped } from "../../model/node";
import { getRootNodeLabel } from "../../standard-query-editor/helper";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import { Button } from "../../ui-components/Button";
import { HoverNavigatable } from "../../ui-components/HoverNavigatable";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../../ui-components/Tooltip";
import { C2, C3 } from "../../ui-components/Typography";

const node = tv({
  base: [
    "grid grid-cols-[1fr_auto]",
    "max-w-[200px]",
    "px-[10px] py-[5px]",
    "cursor-pointer",
    "rounded",
    "hover:bg-bg-100",
    "transition-[background-color] duration-100",
    "[word-break:break-word]",
  ],
  variants: {
    active: {
      true: "border-2 border-primary-500",
      false: "border border-gray-400",
    },
  },
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
          <TooltipTrigger>
            <TooltipTarget as="div" excludeFromTabOrder>
              {rootNodeLabel && (
                <C3 tone="muted" strong>
                  {rootNodeLabel}
                </C3>
              )}
              <C2>{conceptNode?.label}</C2>
              {conceptNode && !!conceptNode.description && (
                <C3 tone="muted">{conceptNode.description}</C3>
              )}
            </TooltipTarget>
            <Tooltip>{tooltipText}</Tooltip>
          </TooltipTrigger>
        </div>
        <div className="ml-[10px]">
          {expand?.expandable && (
            <TooltipTrigger>
              <Button
                aria-label={t("externalForms.common.concept.expand")}
                intent="tertiary"
                size="sm"
                onPress={() => {
                  expand.onClick();
                }}
              >
                {expand.active ? <Minimize2Icon /> : <Maximize2Icon />}
              </Button>
              <Tooltip>{t("externalForms.common.concept.expand")}</Tooltip>
            </TooltipTrigger>
          )}
        </div>
      </div>
    </HoverNavigatable>
  );
};

export default FormConceptNode;
