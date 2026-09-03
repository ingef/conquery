import { memo, type Ref, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { QueryIdT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import { nodeIsConceptQueryNode } from "../model/node";
import DropzoneWithFileInput, {
  type DragItemFile,
} from "../ui-components/DropzoneWithFileInput";

import { EmptyQueryEditorDropzone } from "./EmptyQueryEditorDropzone";
import type { StandardQueryNodeT } from "./types";

const DROP_TYPES = [
  DNDType.CONCEPT_TREE_NODE,
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const dropzone = tv({
  variants: {
    isInitial: { true: "h-full" },
    isAnd: { true: ["h-[100px]", "whitespace-nowrap", "w-[initial]"] },
  },
});

interface Props {
  className?: string;
  isInitial?: boolean;
  isAnd?: boolean;
  onDropNode: (node: StandardQueryNodeT) => void;
  onDropFile: (file: File) => void;
  onLoadPreviousQuery: (id: QueryIdT) => void;
  onImportLines?: (lines: string[], filename?: string) => void;
}

const QueryEditorDropzone = ({
  ref,
  className,
  isAnd,
  isInitial,
  onLoadPreviousQuery,
  onDropFile,
  onDropNode,
  onImportLines,
}: Props & { ref?: Ref<HTMLDivElement> }) => {
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
    <DropzoneWithFileInput
      ref={ref}
      className={dropzone({ isInitial, isAnd, className })}
      isInitial={isInitial}
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDrop(item as StandardQueryNodeT | DragItemFile)}
      onSelectFile={onDropFile}
      disableClick={isInitial}
      showImportButton={isInitial}
      onImportLines={onImportLines}
    >
      {() => (
        <>
          {isInitial && <EmptyQueryEditorDropzone />}
          {!isInitial && (
            <p className="text-sm">{t("dropzone.dragElementPlease")}</p>
          )}
        </>
      )}
    </DropzoneWithFileInput>
  );
};

export default memo(QueryEditorDropzone);
