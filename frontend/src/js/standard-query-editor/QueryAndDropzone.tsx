import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import type { PreviousQueryT } from "../previous-queries/list/reducer";
import WithTooltip from "../tooltip/WithTooltip";

import QueryEditorDropzone from "./QueryEditorDropzone";
import { DragItemConceptTreeNode, DragItemQuery } from "./types";

const PaddedTop = styled("div")`
  padding-top: 70px;
`;

const SxWithTooltip = styled(WithTooltip)`
  display: block !important;
`;

interface Props {
  onDropConceptListFile: (
    file: File,
    andIdx: number | null,
  ) => Promise<unknown>;
  onDropAndNode: (node: DragItemQuery | DragItemConceptTreeNode) => void;
  onLoadQuery: (queryId: PreviousQueryT["id"]) => void;
}

const QueryAndDropzone = ({
  onDropAndNode,
  onDropConceptListFile,
  onLoadQuery,
}: Props) => {
  const { t } = useTranslation();

  return (
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
  );
};

export default memo(QueryAndDropzone);
