import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import T from "i18n-react";
import Hotkeys from "react-hot-keys";

import type { QueryNodeType } from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";

import BasicButton from "../button/BasicButton";

import MenuColumn from "./MenuColumn";
import NodeDetailsView from "./NodeDetailsView";
import TableView from "./TableView";

import { createQueryNodeEditorActions } from "./actions";

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

type QueryNodeEditorState = {
  detailsViewActive: boolean,
  selectedInputTableIdx: number,
  selectedInput: number,
  editingLabel: boolean,

  onSelectDetailsView: Function,
  onSelectInputTableView: Function,
  onShowDescription: Function,
  onToggleEditLabel: Function,
  onReset: Function
};

export type PropsType = {
  name: string,
  editorState: QueryNodeEditorState,
  node: QueryNodeType,
  showTables: boolean,
  isExcludeTimestampsPossible: boolean,
  datasetId: number,
  suggestions: Object | null,
  whitelistedTables?: string[],
  blacklistedTables?: string[],

  onCloseModal: Function,
  onUpdateLabel: Function,
  onDropConcept: Function,
  onRemoveConcept: Function,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onResetAllFilters: Function,
  onToggleTimestamps: Function,
  onSwitchFilterMode: Function,
  onLoadFilterSuggestions: Function,
  onSelectSelects: Function,
  onSelectTableSelects: Function,
  onSetDateColumn: Function
};

const QueryNodeEditor = (props: PropsType) => {
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

export const createConnectedQueryNodeEditor = (
  mapStateToProps: Function,
  mapDispatchToProps: Function,
  mergeProps: Function
) => {
  const mapDispatchToPropsInternal = (dispatch: Dispatch, ownProps) => {
    const externalDispatchProps = mapDispatchToProps
      ? mapDispatchToProps(dispatch, ownProps)
      : {};

    const {
      setDetailsViewActive,
      toggleEditLabel,
      setInputTableViewActive,
      setFocusedInput,
      reset
    } = createQueryNodeEditorActions(ownProps.name);

    return {
      ...externalDispatchProps,
      editorState: {
        ...(externalDispatchProps.editorState || {}),
        onSelectDetailsView: () => dispatch(setDetailsViewActive()),
        onToggleEditLabel: () => dispatch(toggleEditLabel()),
        onSelectInputTableView: tableIdx =>
          dispatch(setInputTableViewActive(tableIdx)),
        onShowDescription: filterIdx => dispatch(setFocusedInput(filterIdx)),
        onReset: () => dispatch(reset())
      }
    };
  };

  const mergePropsInternal = (stateProps, dispatchProps, ownProps) => {
    const externalMergedProps = mergeProps
      ? mergeProps(stateProps, dispatchProps, ownProps)
      : { ...ownProps, ...stateProps, ...dispatchProps };

    return {
      ...externalMergedProps,
      editorState: {
        ...(stateProps.editorState || {}),
        ...(dispatchProps.editorState || {})
      }
    };
  };

  return connect(
    mapStateToProps,
    mapDispatchToPropsInternal,
    mergePropsInternal
  )(QueryNodeEditor);
};
