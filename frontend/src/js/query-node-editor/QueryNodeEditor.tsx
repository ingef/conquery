import React from "react";
import styled from "@emotion/styled";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import Hotkeys from "react-hot-keys";

import type { StandardQueryNodeT } from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";
import { CurrencyConfigT, DatasetIdT } from "../api/types";
import type { ModeT } from "../form-components/InputRange";
import BasicButton from "../button/BasicButton";
import { isConceptQueryNode } from "../model/query";

import MenuColumn from "./MenuColumn";
import NodeDetailsView from "./NodeDetailsView";
import TableView from "./TableView";
import { createQueryNodeEditorActions } from "./actions";
import type { PostPrefixForSuggestionsParams } from "../api/api";
import { QueryNodeEditorStateT } from "./reducer";

const Root = styled("div")`
  margin: 0 10px;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  display: flex;
  background: rgb(249, 249, 249);
  z-index: 1;
`;

const Wrapper = styled("div")`
  border: 1px solid ${({ theme }) => theme.col.blueGrayDark};
  display: flex;
  flex-direction: row;
  width: 100%;
  height: 100%;
  overflow: auto;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SxWithTooltip = styled(WithTooltip)`
  position: absolute;
  bottom: 10px;
  right: 20px;
`;

const CloseButton = styled(BasicButton)`
  border: 1px solid ${({ theme }) => theme.col.blueGrayDark};
`;

export interface QueryNodeEditorPropsT {
  name: string;
  editorState: QueryNodeEditorStateT;
  node: StandardQueryNodeT;
  showTables: boolean;
  isExcludeTimestampsPossible: boolean;
  isExcludeFromSecondaryIdQueryPossible: boolean;
  datasetId: DatasetIdT;
  allowlistedTables?: string[];
  blocklistedTables?: string[];
  currencyConfig: CurrencyConfigT;

  onCloseModal: Function;
  onUpdateLabel: Function;
  onDropConcept: Function;
  onRemoveConcept: Function;
  onToggleTable: Function;
  onSetFilterValue: Function;
  onResetAllFilters: Function;
  onToggleTimestamps: Function;
  onToggleSecondaryIdExclude: Function;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
  ) => void;
  onSelectSelects: Function;
  onSelectTableSelects: Function;
  onSetDateColumn: Function;
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
    onShowDescription: (filterIdx: number) =>
      dispatch(setFocusedInput(filterIdx)),
    onReset: () => dispatch(reset()),
  };

  function close() {
    if (!node) return;

    props.onCloseModal();
    editorState.onReset();
  }

  if (!node) return null;

  const selectedTable =
    isConceptQueryNode(node) && editorState.selectedInputTableIdx != null
      ? node.tables[editorState.selectedInputTableIdx]
      : null;

  return (
    <Root>
      <Wrapper>
        <Hotkeys keyName="escape" onKeyDown={close} />
        <MenuColumn node={node} {...props} editorState={editorState} />
        {editorState.detailsViewActive && (
          <NodeDetailsView node={node} {...props} editorState={editorState} />
        )}
        {isConceptQueryNode(node) &&
          !editorState.detailsViewActive &&
          selectedTable != null && (
            <TableView
              {...props}
              onShowDescription={editorState.onShowDescription}
              datasetId={props.datasetId}
              currencyConfig={props.currencyConfig}
              node={node}
              selectedInputTableIdx={editorState.selectedInputTableIdx}
              onLoadFilterSuggestions={props.onLoadFilterSuggestions}
            />
          )}
        <SxWithTooltip text={t("common.closeEsc")}>
          <CloseButton small onClick={close}>
            {t("common.done")}
          </CloseButton>
        </SxWithTooltip>
      </Wrapper>
    </Root>
  );
};

export default QueryNodeEditor;
