import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { memo, useCallback } from "react";
import { useTranslation } from "react-i18next";

import type { QueryIdT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import { nodeIsConceptQueryNode } from "../model/node";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../ui-components/DropzoneWithFileInput";

import { EmptyQueryEditorDropzone } from "./EmptyQueryEditorDropzone";
import type { StandardQueryNodeT } from "./types";

const DROP_TYPES = [
  DNDType.CONCEPT_TREE_NODE,
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const SxDropzoneWithFileInput = styled(DropzoneWithFileInput)<{
  isInitial?: boolean;
  isAnd?: boolean;
}>`
  ${({ isInitial }) =>
    isInitial &&
    css`
      height: 100%;
    `};

  ${({ isAnd }) =>
    isAnd &&
    css`
      height: 100px;
      white-space: nowrap;
      width: initial;
    `};
`;

const Text = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

interface Props {
  className?: string;
  isInitial?: boolean;
  isAnd?: boolean;
  onDropNode: (node: StandardQueryNodeT) => void;
  onDropFile: (file: File) => void;
  onLoadPreviousQuery: (id: QueryIdT) => void;
}

const QueryEditorDropzone = ({
  className,
  isAnd,
  isInitial,
  onLoadPreviousQuery,
  onDropFile,
  onDropNode,
}: Props) => {
  const { t } = useTranslation();
  const onDrop = useCallback(
    (item: StandardQueryNodeT | DragItemFile) => {
      if (item.type === "__NATIVE_FILE__") {
        onDropFile(item.files[0]);
      } else {
        onDropNode(item);

        if (!nodeIsConceptQueryNode(item)) onLoadPreviousQuery(item.id);
      }
    },
    [onDropFile, onDropNode, onLoadPreviousQuery],
  );

  return (
    <SxDropzoneWithFileInput /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<StandardQueryNodeT>>> */
      className={className}
      isAnd={isAnd}
      isInitial={isInitial}
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDrop(item as StandardQueryNodeT | DragItemFile)}
      onSelectFile={onDropFile}
      disableClick={isInitial}
      showFileSelectButton={isInitial}
    >
      {() => (
        <>
          {isInitial && <EmptyQueryEditorDropzone />}
          {!isInitial && <Text>{t("dropzone.dragElementPlease")}</Text>}
        </>
      )}
    </SxDropzoneWithFileInput>
  );
};

export default memo(QueryEditorDropzone);
