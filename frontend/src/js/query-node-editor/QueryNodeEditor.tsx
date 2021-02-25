import React from "react";
import styled from "@emotion/styled";
import { useDispatch } from "react-redux";
import T from "i18n-react";
import Hotkeys from "react-hot-keys";

import type { QueryNodeType } from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";

import BasicButton from "../button/BasicButton";

import MenuColumn from "./MenuColumn";
import NodeDetailsView from "./NodeDetailsView";
import TableView from "./TableView";

import { createQueryNodeEditorActions } from "./actions";
import { DatasetIdT } from "../api/types";

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

interface QueryNodeEditorState {
  detailsViewActive: boolean;
  selectedInputTableIdx: number;
  selectedInput: number;
  editingLabel: boolean;

  onSelectDetailsView: Function;
  onSelectInputTableView: Function;
  onShowDescription: Function;
  onToggleEditLabel: Function;
  onReset: Function;
}

export interface PropsType {
  name: string;
  editorState: QueryNodeEditorState;
  node: QueryNodeType;
  showTables: boolean;
  isExcludeTimestampsPossible: boolean;
  isExcludeFromSecondaryIdQueryPossible: boolean;
  datasetId: DatasetIdT;
  suggestions: Object | null;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onCloseModal: Function;
  onUpdateLabel: Function;
  onDropConcept: Function;
  onRemoveConcept: Function;
  onToggleTable: Function;
  onSetFilterValue: Function;
  onResetAllFilters: Function;
  onToggleTimestamps: Function;
  onToggleSecondaryIdExclude: Function;
  onSwitchFilterMode: Function;
  onLoadFilterSuggestions: Function;
  onSelectSelects: Function;
  onSelectTableSelects: Function;
  onSetDateColumn: Function;
}

const QueryNodeEditorComponent = (props: PropsType) => {
  const { node, editorState } = props;

  function close() {
    if (!node) return;

    props.onCloseModal();
    editorState.onReset();
  }

  if (!node) return null;

  const selectedTable =
    !node.isPreviousQuery && editorState.selectedInputTableIdx != null
      ? node.tables[editorState.selectedInputTableIdx]
      : null;

  return (
    <Root>
      <Wrapper>
        <Hotkeys keyName="escape" onKeyDown={close} />
        <MenuColumn {...props} />
        {editorState.detailsViewActive && <NodeDetailsView {...props} />}
        {!editorState.detailsViewActive && selectedTable != null && (
          <TableView {...props} />
        )}
        <SxWithTooltip text={T.translate("common.closeEsc")}>
          <CloseButton small onClick={close}>
            {T.translate("common.done")}
          </CloseButton>
        </SxWithTooltip>
      </Wrapper>
    </Root>
  );
};

const QueryNodeEditor = (props: PropsType) => {
  const dispatch = useDispatch();

  const {
    setDetailsViewActive,
    toggleEditLabel,
    setInputTableViewActive,
    setFocusedInput,
    reset,
  } = createQueryNodeEditorActions(props.name);

  return (
    <QueryNodeEditorComponent
      {...props}
      editorState={{
        ...(props.editorState || {}),
        onSelectDetailsView: () => dispatch(setDetailsViewActive()),
        onToggleEditLabel: () => dispatch(toggleEditLabel()),
        onSelectInputTableView: (tableIdx: number) =>
          dispatch(setInputTableViewActive(tableIdx)),
        onShowDescription: (filterIdx: number) =>
          dispatch(setFocusedInput(filterIdx)),
        onReset: () => dispatch(reset()),
      }}
    />
  );
};

export default QueryNodeEditor;
