import styled from "@emotion/styled";
import { useCallback, useRef, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  ConceptIdT,
  DatasetT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { useResizeObserver } from "../common/helpers/useResizeObserver";
import {
  nodeHasEmptySettings,
  nodeIsConceptQueryNode,
  NodeResetConfig,
} from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import type { ModeT } from "../ui-components/InputRange";

import ContentColumn from "./ContentColumn";
import MenuColumn from "./MenuColumn";
import NodeName from "./NodeName";
import ResetAndClose from "./ResetAndClose";
import { useAutoLabel } from "./useAutoLabel";

const Root = styled("div")`
  padding: 10px;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  z-index: 1;
  background-color: ${({ theme }) => theme.col.bg};
`;

const ContentWrap = styled("div")`
  background-color: white;
  box-shadow: 1px 2px 5px 0 rgba(0, 0, 0, 0.2);
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  border-radius: ${({ theme }) => theme.borderRadius};
  flex-grow: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
`;

const Wrapper = styled("div")`
  flex-grow: 1;
  width: 100%;
  overflow: hidden;
`;
const ScrollContainer = styled("div")`
  position: relative;
  display: flex;
  flex-direction: row;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background-color: ${({ theme }) => theme.col.bg};
  --webkit-overflow-scrolling: touch;
`;

const SxMenuColumn = styled(MenuColumn)`
  background-color: ${({ theme }) => theme.col.bg};
  position: sticky;
  z-index: 2;
  top: 0;
  left: 0;
`;

const Header = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border-bottom: 1px solid #ccc;
  padding-right: 10px;
`;

export interface QueryNodeEditorPropsT {
  name: string;
  node: StandardQueryNodeT;
  showTables: boolean;
  datasetId: DatasetT["id"];
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
  onSetFilterValue: (tableIdx: number, filterIdx: number, value: any) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
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
const RIGHT_SIDE_WIDTH = 400;
const RIGHT_SIDE_WIDTH_COMPACT = 150;

const QueryNodeEditor = ({ node, ...props }: QueryNodeEditorPropsT) => {
  const [selectedTableIdx, setSelectedTableIdx] = useState<number | null>(null);

  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const onCommonSettingsClick = () => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // To make sure that Close button is always visible and to consider
  // that QueryNodeEditor may be contained in a horizontally resizeable panel
  // that's resized independent of the window width.
  // TODO: Once https://caniuse.com/css-container-queries ships, use those instead
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
  const nodeNameMaxWidth =
    parentWidth -
    (isCompact || !showClearReset
      ? RIGHT_SIDE_WIDTH_COMPACT
      : RIGHT_SIDE_WIDTH);

  const { autoLabel, autoLabelEnabled, setAutoLabelEnabled } = useAutoLabel({
    node,
    onUpdateLabel: props.onUpdateLabel,
  });

  return (
    <Root
      ref={(instance) => {
        if (instance && parentWidth === 0) {
          setParentWidth(instance.getBoundingClientRect().width);
        }
        parentRef.current = instance;
      }}
    >
      <ContentWrap>
        <Header>
          <NodeName
            maxWidth={nodeNameMaxWidth}
            allowEditing={nodeIsConceptQueryNode(node)}
            label={
              autoLabelEnabled && autoLabel
                ? autoLabel
                : nodeIsConceptQueryNode(node)
                ? node.label
                : node.label || node.id
            }
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
        </Header>
        <Wrapper>
          <ScrollContainer ref={scrollContainerRef}>
            <SxMenuColumn
              node={node}
              selectedTableIdx={selectedTableIdx}
              showTables={props.showTables}
              blocklistedTables={props.blocklistedTables}
              allowlistedTables={props.allowlistedTables}
              onCommonSettingsClick={onCommonSettingsClick}
              onDropConcept={props.onDropConcept}
              onRemoveConcept={props.onRemoveConcept}
              onToggleTable={(tableIdx, isExcluded) => {
                if (isExcluded && selectedTableIdx === tableIdx) {
                  setSelectedTableIdx(null);
                }

                props.onToggleTable(tableIdx, isExcluded);
              }}
              onSelectTable={setSelectedTableIdx}
              onResetTable={props.onResetTable}
            />
            <ContentColumn
              node={node}
              datasetId={props.datasetId}
              selectedTableIdx={selectedTableIdx}
              allowlistedSelects={props.allowlistedSelects}
              blocklistedSelects={props.blocklistedSelects}
              onToggleTimestamps={props.onToggleTimestamps}
              onToggleSecondaryIdExclude={props.onToggleSecondaryIdExclude}
              onSelectSelects={props.onSelectSelects}
              onSelectTableSelects={props.onSelectTableSelects}
              onLoadFilterSuggestions={props.onLoadFilterSuggestions}
              onSetDateColumn={props.onSetDateColumn}
              onSetFilterValue={props.onSetFilterValue}
              onSwitchFilterMode={props.onSwitchFilterMode}
            />
          </ScrollContainer>
        </Wrapper>
      </ContentWrap>
    </Root>
  );
};

export default QueryNodeEditor;
