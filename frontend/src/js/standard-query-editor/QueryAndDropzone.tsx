import { memo } from "react";
import { useTranslation } from "react-i18next";

import type { PreviousQueryT } from "../previous-queries/list/reducer";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";

import QueryEditorDropzone from "./QueryEditorDropzone";
import type { DragItemConceptTreeNode, DragItemQuery } from "./types";

interface Props {
  onDropFile: (file: File, andIdx?: number) => Promise<unknown>;
  onDropAndNode: (node: DragItemQuery | DragItemConceptTreeNode) => void;
  onLoadQuery: (queryId: PreviousQueryT["id"]) => void;
  onImportLines: (lines: string[]) => void;
}

const QueryAndDropzone = ({
  onDropAndNode,
  onDropFile,
  onLoadQuery,
  onImportLines,
}: Props) => {
  const { t } = useTranslation();

  return (
    <div className="pt-[70px]">
      <TooltipTrigger delay={1500}>
        <TooltipTarget as="div" excludeFromTabOrder>
          <QueryEditorDropzone
            isAnd
            onDropNode={onDropAndNode}
            onDropFile={onDropFile}
            onImportLines={onImportLines}
            onLoadPreviousQuery={onLoadQuery}
          />
        </TooltipTarget>
        <Tooltip>{t("help.editorDropzoneAnd")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(QueryAndDropzone);
