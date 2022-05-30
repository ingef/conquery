import styled from "@emotion/styled";
import { useCallback, useRef, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { TransparentButton } from "../button/TransparentButton";
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
import WithTooltip from "../tooltip/WithTooltip";
import EditableText from "../ui-components/EditableText";
import type { ModeT } from "../ui-components/InputRange";

import ContentColumn from "./ContentColumn";
import MenuColumn from "./MenuColumn";
import ResetAllSettingsButton from "./ResetAllSettingsButton";

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

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const CloseButton = styled(TransparentButton)``;

const NodeName = styled("div")`
  padding: 10px 15px;
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
  currencyConfig: CurrencyConfigT;

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
  const { t } = useTranslation();
  const [editingLabel, setEditingLabel] = useState<boolean>(false);
  const [selectedTableIdx, setSelectedTableIdx] = useState<number | null>(null);

  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const onCommonSettingsClick = () => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  function close() {
    if (!node) return;

    props.onCloseModal();
  }

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

  useHotkeys("esc", close);

  if (!node) return null;

  const showClearReset = !nodeHasEmptySettings(node);

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
            style={{
              maxWidth:
                parentWidth -
                (isCompact || !showClearReset
                  ? RIGHT_SIDE_WIDTH_COMPACT
                  : RIGHT_SIDE_WIDTH),
            }}
          >
            {nodeIsConceptQueryNode(node) && (
              <EditableText
                large
                loading={false}
                text={node.label}
                tooltip={t("help.editConceptName")}
                selectTextOnMount={true}
                editing={editingLabel}
                onSubmit={(value) => {
                  props.onUpdateLabel(value);
                  setEditingLabel(false);
                }}
                onToggleEdit={() => setEditingLabel(!editingLabel)}
              />
            )}
            {!nodeIsConceptQueryNode(node) && (node.label || node.id)}
          </NodeName>
          <Row>
            {showClearReset && (
              <ResetAllSettingsButton
                text={t("queryNodeEditor.clearAllSettings")}
                icon="trash"
                onClick={() => props.onResetAllSettings({ useDefaults: false })}
                compact={isCompact}
              />
            )}
            <WithTooltip text={t("common.saveAndCloseEsc")}>
              <CloseButton small onClick={close}>
                {t("common.save")}
              </CloseButton>
            </WithTooltip>
          </Row>
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
              currencyConfig={props.currencyConfig}
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
