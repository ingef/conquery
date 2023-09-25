import styled from "@emotion/styled";
import { useCallback, useMemo, useState, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { QueryT } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { getUniqueFileRows } from "../common/helpers/fileHelper";
import { TreesT } from "../concept-trees/reducer";
import { useDatasetId } from "../dataset/selectors";
import { useLoadQuery } from "../previous-queries/list/actions";
import { PreviousQueryT } from "../previous-queries/list/reducer";
import QueryGroupModal from "../query-group-modal/QueryGroupModal";
import QueryUploadConceptListModal from "../query-upload-concept-list-modal/QueryUploadConceptListModal";
import { initUploadConceptListModal } from "../upload-concept-list-modal/actions";

import ExpandPreviousQueryModal from "./ExpandPreviousQueryModal";
import QueryAndDropzone from "./QueryAndDropzone";
import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroup from "./QueryGroup";
import QueryHeader from "./QueryHeader";
import SecondaryIdSelector from "./SecondaryIdSelector";
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
import type { DragItemConceptTreeNode, DragItemQuery } from "./types";

const useImport = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [dropFileModalOpen, setDropFileModalOpen] = useState(false);
  const [dropFileModalAndIdx, setFileDropModalAndIdx] = useState<
    number | undefined
  >();

  const onDropFile = useCallback(
    async (file: File, andIdx?: number) => {
      // Wait until file is processed before opening modal
      const rows = await getUniqueFileRows(file);

      dispatch(initUploadConceptListModal({ rows, filename: file.name }));

      setDropFileModalOpen(true);
      setFileDropModalAndIdx(andIdx);
    },
    [dispatch],
  );

  const onCloseDropFileModal = useCallback(() => {
    setDropFileModalOpen(false);
    setFileDropModalAndIdx(undefined);
  }, []);

  const onImportLines = useCallback(
    (lines: string[], filename?: string, andIdx?: number) => {
      dispatch(
        initUploadConceptListModal({
          rows: lines,
          filename: filename || t("importModal.pasted"),
        }),
      );

      setDropFileModalOpen(true);
      setFileDropModalAndIdx(andIdx);
    },
    [t, dispatch],
  );

  return {
    dropFileModalOpen,
    dropFileModalAndIdx,
    onCloseDropFileModal,
    onDropFile,
    onImportLines,
  };
};

const Container = styled("div")`
  height: 100%;
  display: flex;
  flex-direction: column;
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
  const datasetId = useDatasetId();
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );
  const isEmptyQuery = useMemo(() => query.length === 0, [query.length]);
  const isQueryWithSingleElement =
    query.length === 1 && query[0].elements.length === 1;

  // only used by other actions
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  const dispatch = useDispatch();
  const { loadQuery } = useLoadQuery();
  const expandPreviousQuery = useExpandPreviousQuery();

  const {
    dropFileModalOpen,
    dropFileModalAndIdx,
    onCloseDropFileModal,
    onDropFile,
    onImportLines,
  } = useImport();

  const onDropAndNode = useCallback(
    (item: DragItemQuery | DragItemConceptTreeNode) =>
      dispatch(dropAndNode({ item })),
    [dispatch],
  );
  const onDropOrNode = useCallback(
    (item: DragItemQuery | DragItemConceptTreeNode, andIdx: number) =>
      dispatch(dropOrNode({ item, andIdx })),
    [dispatch],
  );
  const onDeleteNode = useCallback(
    (andIdx: number, orIdx: number) => dispatch(deleteNode({ andIdx, orIdx })),
    [dispatch],
  );
  const onDeleteGroup = useCallback(
    (andIdx: number) => dispatch(deleteGroup({ andIdx })),
    [dispatch],
  );
  const onToggleExcludeGroup = useCallback(
    (andIdx: number) => dispatch(toggleExcludeGroup({ andIdx })),
    [dispatch],
  );
  const onToggleTimestamps = useCallback(
    (andIdx: number, orIdx: number) =>
      dispatch(toggleTimestamps({ andIdx, orIdx })),
    [dispatch],
  );
  const onToggleSecondaryIdExclude = useCallback(
    (andIdx: number, orIdx: number) =>
      dispatch(toggleSecondaryIdExclude({ andIdx, orIdx })),
    [dispatch],
  );
  const onLoadQuery = useCallback(
    (queryId: PreviousQueryT["id"]) => {
      loadQuery(queryId);
    },
    [loadQuery],
  );

  const [queryToExpand, setQueryToExpand] = useState<QueryT | null>(null);

  const [queryGroupModalAndIx, setQueryGroupModalAndIdx] = useState<
    number | null
  >(null);

  const onEditClick = useCallback(
    (andIdx: number, orIdx: number) => {
      setEditedNode({ andIdx, orIdx });
    },
    [setEditedNode],
  );

  const onExpandPreviousQuery = useCallback(
    (q: QueryT) => {
      if (isQueryWithSingleElement) {
        expandPreviousQuery(rootConcepts, q);
      } else {
        setQueryToExpand(q);
      }
    },
    [expandPreviousQuery, isQueryWithSingleElement, rootConcepts],
  );

  if (!datasetId) {
    return null;
  }

  return (
    <Container>
      {dropFileModalOpen && (
        <QueryUploadConceptListModal
          andIdx={dropFileModalAndIdx}
          onClose={onCloseDropFileModal}
        />
      )}
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
            expandPreviousQuery(rootConcepts, queryToExpand);
            setQueryToExpand(null);
          }}
        />
      )}
      {isEmptyQuery ? (
        <QueryEditorDropzone
          isInitial
          onDropNode={onDropAndNode}
          onDropFile={onDropFile}
          onLoadPreviousQuery={onLoadQuery}
          onImportLines={onImportLines}
        />
      ) : (
        <>
          <QueryHeader />
          <Groups>
            {query.map((group, andIdx) => (
              <Fragment key={andIdx}>
                <QueryGroup
                  key={andIdx}
                  group={group}
                  andIdx={andIdx}
                  onDropOrNode={onDropOrNode}
                  onDropFile={onDropFile}
                  onImportLines={onImportLines}
                  onDeleteGroup={onDeleteGroup}
                  onExcludeClick={onToggleExcludeGroup}
                  onDateClick={setQueryGroupModalAndIdx}
                  onExpandClick={onExpandPreviousQuery}
                  onLoadPreviousQuery={onLoadQuery}
                  onDeleteNode={onDeleteNode}
                  onEditClick={onEditClick}
                  onToggleTimestamps={onToggleTimestamps}
                  onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
                />
                <QueryGroupConnector key={`${andIdx}.and`}>
                  {t("common.and")}
                </QueryGroupConnector>
              </Fragment>
            ))}
            <QueryAndDropzone
              onLoadQuery={onLoadQuery}
              onDropAndNode={onDropAndNode}
              onDropFile={onDropFile}
              onImportLines={onImportLines}
            />
          </Groups>
          <SecondaryIdSelector />
        </>
      )}
    </Container>
  );
};

export default Query;
