import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC } from "react";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";

import type { QueryIdT } from "../api/types";
import {
  CONCEPT_TREE_NODE,
  QUERY_NODE,
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../common/constants/dndTypes";
import FaIcon from "../icon/FaIcon";
import { DropzoneProps } from "../ui-components/Dropzone";
import DropzoneWithFileInput from "../ui-components/DropzoneWithFileInput";

import type {
  DragItemConceptTreeNode,
  DragItemNode,
  DragItemQuery,
} from "./types";

const DROP_TYPES = [
  CONCEPT_TREE_NODE,
  QUERY_NODE,
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
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
const TextInitial = styled("div")`
  width: 100%;
  font-size: ${({ theme }) => theme.font.lg};
  padding: 30px;
  font-weight: 400;

  p {
    margin: 0;
  }
  ul {
    margin: 0;
    padding: 0 22px;
  }
  h2 {
    font-size: ${({ theme }) => theme.font.huge};
    line-height: 1.3;
    margin: 0 0 20px;
  }
`;

const ArrowRight = styled(FaIcon)`
  font-size: 140px;
  margin-right: 30px;
  color: ${({ theme }) => theme.col.grayLight};
`;
const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

interface PropsT {
  className?: string;
  isInitial?: boolean;
  isAnd?: boolean;
  onDropNode: (
    node: DragItemQuery | DragItemNode | DragItemConceptTreeNode,
  ) => void;
  onDropFile: (file: File) => void;
  onLoadPreviousQuery: (id: QueryIdT) => void;
}

const QueryEditorDropzone: FC<PropsT> = ({
  className,
  isAnd,
  isInitial,
  onLoadPreviousQuery,
  onDropFile,
  onDropNode,
}) => {
  const { t } = useTranslation();

  return (
    <SxDropzoneWithFileInput<
      FC<DropzoneProps<DragItemConceptTreeNode | DragItemNode | DragItemQuery>>
    >
      className={className}
      isAnd={isAnd}
      isInitial={isInitial}
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => {
        if (item.type === "__NATIVE_FILE__") {
          onDropFile(item.files[0]);
        } else {
          onDropNode(item);

          if (item.isPreviousQuery) onLoadPreviousQuery(item.id);
        }
      }}
      onSelectFile={onDropFile}
      disableClick={isInitial}
      showFileSelectButton={isInitial}
    >
      {() => (
        <>
          {isInitial && (
            <TextInitial>
              <h2>{t("dropzone.explanation")}</h2>
              <Row>
                <ArrowRight icon="arrow-right" />
                <div>
                  <p>{t("dropzone.drop")}</p>
                  <ul>
                    <li>{t("dropzone.aConcept")}</li>
                    <li>{t("dropzone.aQuery")}</li>
                    <li>{t("dropzone.aConceptList")}</li>
                  </ul>
                  <p>{t("dropzone.intoThisArea")}</p>
                </div>
              </Row>
            </TextInitial>
          )}
          {!isInitial && <Text>{t("dropzone.dragElementPlease")}</Text>}
        </>
      )}
    </SxDropzoneWithFileInput>
  );
};

export default QueryEditorDropzone;
