import { useCallback, useMemo, useRef, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { tv } from "tailwind-variants";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  ConceptIdT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { useResizeObserver } from "../common/helpers/useResizeObserver";
import {
  type NodeResetConfig,
  nodeHasEmptySettings,
  nodeIsConceptQueryNode,
} from "../model/node";
import type {
  DragItemConceptTreeNode,
  FilterWithValueType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import ContentColumn from "./ContentColumn";
import MenuColumn from "./MenuColumn";
import NodeName from "./NodeName";
import ResetAndClose from "./ResetAndClose";
import { useAutoLabel } from "./useAutoLabel";
import { useSectionSpy } from "./useSectionSpy";

const root = tv({
  base: ["absolute inset-0", "z-2", "p-2", "bg-bg-50"],
});

const contentWrap = tv({
  base: [
    "flex flex-col",
    "grow",
    "h-full w-full",
    "overflow-hidden",
    "rounded",
    "border border-gray-400",
    "bg-white",
    "shadow-[1px_2px_5px_0_rgba(0,0,0,0.2)]",
  ],
});

// the original also declared `--webkit-overflow-scrolling: touch` —
// a typo (double dash) that only defined an unused custom property, dropped
const scrollContainer = tv({
  base: [
    "relative",
    "flex flex-row",
    "h-full w-full",
    "overflow-y-auto",
    "bg-bg-50",
  ],
});

const menuColumn = tv({
  base: ["sticky top-0 left-0", "z-2", "bg-bg-50"],
});

const header = tv({
  base: [
    "flex items-center justify-between",
    "gap-4",
    "h-10 shrink-0",
    "px-2",
    "border-b border-gray-100",
  ],
});

interface QueryNodeEditorPropsT {
  name: string;
  node: StandardQueryNodeT;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];
  allowlistedSelects?: SelectorResultType[];
  blocklistedSelects?: SelectorResultType[];

  onCloseModal: () => void;
  onUpdateLabel: (label: string) => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onResetAllSettings: (config: NodeResetConfig) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
  onToggleTimestamps?: () => void;
  onToggleSecondaryIdExclude?: () => void;
  onSetFilterValue: (
    tableIdx: number,
    filterIdx: number,
    value: FilterWithValueType["value"],
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
  onSetDateColumn: (tableIdx: number, value: string) => void;
  onSelectSelects: (value: SelectOptionT[]) => void;
  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
}

const COMPACT_WIDTH = 600;

const QueryNodeEditor = ({ node, ...props }: QueryNodeEditorPropsT) => {
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const { activeSection, registerSection, registerSentinel, scrollToSection } =
    useSectionSpy(scrollContainerRef);

  // no container query: compact mode also swaps in a tooltip
  const parentRef = useRef<HTMLDivElement | null>(null);
  const [parentWidth, setParentWidth] = useState<number>(0);
  const isCompact = parentWidth < COMPACT_WIDTH;
  useResizeObserver(
    useCallback((entry: ResizeObserverEntry) => {
      if (entry) {
        setParentWidth(entry.contentRect.width);
      }
    }, []),
    parentRef.current,
  );

  useHotkeys("esc", props.onCloseModal);

  const showClearReset = !nodeHasEmptySettings(node);

  const { autoLabel, autoLabelEnabled, setAutoLabelEnabled } = useAutoLabel({
    node,
    onUpdateLabel: props.onUpdateLabel,
  });
  const nodeLabel = useMemo(
    () =>
      !nodeIsConceptQueryNode(node)
        ? node.label || node.id
        : autoLabelEnabled && autoLabel && node.ids.length > 1
          ? autoLabel
          : node.label,
    [autoLabel, autoLabelEnabled, node],
  );

  return (
    <div
      className={root()}
      ref={(instance) => {
        if (instance && parentWidth === 0) {
          setParentWidth(instance.getBoundingClientRect().width);
        }
        parentRef.current = instance;
      }}
    >
      <div className={contentWrap()}>
        <div className={header()}>
          <NodeName
            allowEditing={nodeIsConceptQueryNode(node)}
            label={nodeLabel}
            onUpdateLabel={(label) => {
              setAutoLabelEnabled(false);
              props.onUpdateLabel(label);
            }}
          />
          <ResetAndClose
            isCompact={isCompact}
            onClose={props.onCloseModal}
            onResetAllSettings={props.onResetAllSettings}
            showClearReset={showClearReset}
          />
        </div>
        <div className="w-full grow overflow-hidden">
          <div className={scrollContainer()} ref={scrollContainerRef}>
            <MenuColumn
              className={menuColumn()}
              node={node}
              activeSection={activeSection}
              showTables={props.showTables}
              blocklistedTables={props.blocklistedTables}
              allowlistedTables={props.allowlistedTables}
              onDropConcept={props.onDropConcept}
              onRemoveConcept={props.onRemoveConcept}
              onToggleTable={props.onToggleTable}
              onSelectSection={scrollToSection}
              onResetTable={props.onResetTable}
            />
            <ContentColumn
              node={node}
              registerSection={registerSection}
              registerSentinel={registerSentinel}
              allowlistedSelects={props.allowlistedSelects}
              blocklistedSelects={props.blocklistedSelects}
              onToggleTimestamps={props.onToggleTimestamps}
              onToggleSecondaryIdExclude={props.onToggleSecondaryIdExclude}
              onSelectSelects={props.onSelectSelects}
              onSelectTableSelects={props.onSelectTableSelects}
              onLoadFilterSuggestions={props.onLoadFilterSuggestions}
              onSetDateColumn={props.onSetDateColumn}
              onSetFilterValue={props.onSetFilterValue}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default QueryNodeEditor;
