import styled from "@emotion/styled";
import React from "react";
import Hotkeys from "react-hot-keys";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import {
  ConceptIdT,
  CurrencyConfigT,
  DatasetIdT,
  SelectOptionT,
} from "../api/types";
import TransparentButton from "../button/TransparentButton";
import EditableText from "../form-components/EditableText";
import type { ModeT } from "../form-components/InputRange";
import { nodeIsConceptQueryNode } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";

import ContentColumn from "./ContentColumn";
import MenuColumn from "./MenuColumn";
import ResetAllFiltersButton from "./ResetAllFiltersButton";
import { createQueryNodeEditorActions } from "./actions";
import { QueryNodeEditorStateT } from "./reducer";

const Root = styled("div")`
  padding: 0 20px 10px;
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
  editorState: QueryNodeEditorStateT;
  node: StandardQueryNodeT;
  showTables: boolean;
  datasetId: DatasetIdT;
  allowlistedTables?: string[];
  blocklistedTables?: string[];
  currencyConfig: CurrencyConfigT;

  onCloseModal: () => void;
  onUpdateLabel: (label: string) => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onResetAllFilters: () => void;
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
  ) => void;
  onSetDateColumn: (tableIdx: number, value: string | null) => void;
  onSelectSelects: (value: SelectOptionT[]) => void;
  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
}

const QueryNodeEditor = ({ node, ...props }: QueryNodeEditorPropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const {
    setDetailsViewActive,
    toggleEditLabel,
    setInputTableViewActive,
    setFocusedInput,
    reset,
  } = createQueryNodeEditorActions(props.name);

  // TODO: Move all of the callbacks out of that object and pass individually where necessary
  const editorState = {
    ...(props.editorState || {}),
    onSelectDetailsView: () => dispatch(setDetailsViewActive()),
    onToggleEditLabel: () => dispatch(toggleEditLabel()),
    onSelectInputTableView: (tableIdx: number) =>
      dispatch(setInputTableViewActive(tableIdx)),
    onReset: () => dispatch(reset()),
  };

  const onShowDescription = (filterIdx: number) =>
    dispatch(setFocusedInput(filterIdx));

  function close() {
    if (!node) return;

    props.onCloseModal();
    editorState.onReset();
  }

  if (!node) return null;

  return (
    <Root>
      <ContentWrap>
        <Hotkeys keyName="escape" onKeyDown={close} />
        <Header>
          <NodeName>
            {nodeIsConceptQueryNode(node) && (
              <EditableText
                large
                loading={false}
                text={node.label}
                tooltip={t("help.editConceptName")}
                selectTextOnMount={true}
                editing={editorState.editingLabel}
                onSubmit={(value) => {
                  props.onUpdateLabel(value);
                  editorState.onToggleEditLabel();
                }}
                onToggleEdit={editorState.onToggleEditLabel}
              />
            )}
            {node.isPreviousQuery && (node.label || node.id || node.ids)}
          </NodeName>
          <Row>
            <ResetAllFiltersButton
              node={node}
              onResetAllFilters={props.onResetAllFilters}
            />
            <WithTooltip text={t("common.closeEsc")}>
              <CloseButton small onClick={close}>
                {t("common.close")}
              </CloseButton>
            </WithTooltip>
          </Row>
        </Header>
        <Wrapper>
          <ScrollContainer>
            <SxMenuColumn
              node={node}
              editorState={editorState}
              showTables={props.showTables}
              blocklistedTables={props.blocklistedTables}
              allowlistedTables={props.allowlistedTables}
              onDropConcept={props.onDropConcept}
              onRemoveConcept={props.onRemoveConcept}
              onToggleTable={props.onToggleTable}
            />
            <ContentColumn
              node={node}
              datasetId={props.datasetId}
              currencyConfig={props.currencyConfig}
              selectedTableIdx={props.editorState.selectedInputTableIdx}
              onShowDescription={onShowDescription}
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
