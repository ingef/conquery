import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";
import { getUniqueFileRows } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import { TreesT } from "../concept-trees/reducer";
import { useLoadQuery } from "../previous-queries/list/actions";
import { PreviousQueryIdT } from "../previous-queries/list/reducer";
import QueryGroupModal from "../query-group-modal/QueryGroupModal";
import { openQueryUploadConceptListModal } from "../query-upload-concept-list-modal/actions";
import WithTooltip from "../tooltip/WithTooltip";
import { initUploadConceptListModal } from "../upload-concept-list-modal/actions";

import ExpandPreviousQueryModal from "./ExpandPreviousQueryModal";
import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryFooter from "./QueryFooter";
import QueryGroup from "./QueryGroup";
import QueryHeader from "./QueryHeader";
import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  useExpandPreviousQuery,
  toggleTimestamps,
  toggleSecondaryIdExclude,
} from "./actions";
import type { StandardQueryStateT } from "./queryReducer";
import type {
  DragItemConceptTreeNode,
  DragItemNode,
  DragItemQuery,
  PreviousQueryQueryNodeType,
} from "./types";

const Container = styled("div")`
  height: 100%;
  display: flex;
  flex-direction: column;
`;

const PaddedTop = styled("div")`
  padding-top: 70px;
`;

const SxWithTooltip = styled(WithTooltip)`
  display: block !important;
`;

const Groups = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  margin: 0 0 10px;
  flex-grow: 1;
  overflow: auto;
`;

const QueryGroupConnector = styled("p")`
  padding: 110px 6px 0;
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
`;

const Query = ({
  setEditedNode,
}: {
  setEditedNode: (node: { andIdx: number; orIdx: number } | null) => void;
}) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );
  const isEmptyQuery = query.length === 0;
  const isQueryWithSingleElement =
    query.length === 1 && query[0].elements.length === 1;

  // only used by other actions
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  const dispatch = useDispatch();
  const loadQuery = useLoadQuery();
  const expandPreviousQuery = useExpandPreviousQuery();

  const onDropAndNode = (
    item: DragItemNode | DragItemQuery | DragItemConceptTreeNode,
  ) => dispatch(dropAndNode({ item }));
  const onDropConceptListFile = async (file: File, andIdx: number | null) => {
    // Need to wait until file is processed.
    // Because if file is empty, modal would close automatically
    const rows = await getUniqueFileRows(file);

    dispatch(initUploadConceptListModal({ rows, filename: file.name }));

    return dispatch(openQueryUploadConceptListModal({ andIdx }));
  };
  const onDropOrNode = (
    item: DragItemNode | DragItemQuery | DragItemConceptTreeNode,
    andIdx: number,
  ) => dispatch(dropOrNode({ item, andIdx }));
  const onDeleteNode = (andIdx: number, orIdx: number) =>
    dispatch(deleteNode({ andIdx, orIdx }));
  const onDeleteGroup = (andIdx: number) => dispatch(deleteGroup({ andIdx }));
  const onToggleExcludeGroup = (andIdx: number) =>
    dispatch(toggleExcludeGroup({ andIdx }));
  const onToggleTimestamps = (andIdx: number, orIdx: number) =>
    dispatch(toggleTimestamps({ andIdx, orIdx }));
  const onToggleSecondaryIdExclude = (andIdx: number, orIdx: number) =>
    dispatch(toggleSecondaryIdExclude({ andIdx, orIdx }));
  const onLoadQuery = (queryId: PreviousQueryIdT) => {
    if (datasetId) {
      loadQuery(datasetId, queryId);
    }
  };

  const [
    queryToExpand,
    setQueryToExpand,
  ] = useState<PreviousQueryQueryNodeType | null>(null);

  const [queryGroupModalAndIx, setQueryGroupModalAndIdx] = useState<
    number | null
  >(null);

  if (!datasetId) {
    return null;
  }

  const onExpandPreviousQuery = (q: PreviousQueryQueryNodeType) => {
    if (isQueryWithSingleElement) {
      expandPreviousQuery(datasetId, rootConcepts, q);
    } else {
      setQueryToExpand(q);
    }
  };

  return (
    <Container>
      {exists(queryGroupModalAndIx) && (
        <QueryGroupModal
          andIdx={queryGroupModalAndIx}
          onClose={() => setQueryGroupModalAndIdx(null)}
        />
      )}
      {exists(queryToExpand) && (
        <ExpandPreviousQueryModal
          onClose={() => setQueryToExpand(null)}
          onAccept={() => {
            if (datasetId) {
              expandPreviousQuery(datasetId, rootConcepts, queryToExpand);
              setQueryToExpand(null);
            }
          }}
        />
      )}
      {isEmptyQuery ? (
        <QueryEditorDropzone
          isInitial
          onDropNode={onDropAndNode}
          onDropFile={(file) => onDropConceptListFile(file, null)}
          onLoadPreviousQuery={onLoadQuery}
        />
      ) : (
        <>
          <QueryHeader />
          <Groups>
            {query.map((group, andIdx) => [
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={(item) => onDropOrNode(item, andIdx)}
                onDropFile={(file: File) => onDropConceptListFile(file, andIdx)}
                onDeleteNode={(orIdx: number) => onDeleteNode(andIdx, orIdx)}
                onDeleteGroup={() => onDeleteGroup(andIdx)}
                onEditClick={(orIdx: number) =>
                  setEditedNode({ andIdx, orIdx })
                }
                onExpandClick={onExpandPreviousQuery}
                onExcludeClick={() => onToggleExcludeGroup(andIdx)}
                onDateClick={() => setQueryGroupModalAndIdx(andIdx)}
                onLoadPreviousQuery={onLoadQuery}
                onToggleTimestamps={(orIdx: number) =>
                  onToggleTimestamps(andIdx, orIdx)
                }
                onToggleSecondaryIdExclude={(orIdx: number) =>
                  onToggleSecondaryIdExclude(andIdx, orIdx)
                }
              />,
              <QueryGroupConnector key={`${andIdx}.and`}>
                {t("common.and")}
              </QueryGroupConnector>,
            ])}
            <PaddedTop>
              <SxWithTooltip text={t("help.editorDropzoneAnd")} lazy>
                <QueryEditorDropzone
                  isAnd
                  onDropNode={onDropAndNode}
                  onDropFile={(file) => onDropConceptListFile(file, null)}
                  onLoadPreviousQuery={onLoadQuery}
                />
              </SxWithTooltip>
            </PaddedTop>
          </Groups>
          <QueryFooter />
        </>
      )}
    </Container>
  );
};

export default Query;
