import React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import T from "i18n-react";

import DropzoneWithFileInput from "../form-components/DropzoneWithFileInput";
import FaIcon from "../icon/FaIcon";

import * as dndTypes from "../common/constants/dndTypes";
import type { QueryIdT } from "../api/types";
import type { DraggedNodeType, DraggedQueryType } from "./types";

type DraggedFileType = Object;

interface PropsT {
  isInitial?: boolean;
  isAnd?: boolean;
  onDropNode: (node: DraggedNodeType | DraggedQueryType) => void;
  onDropFile: (file: DraggedFileType) => void;
  onLoadPreviousQuery: (id: QueryIdT) => void;
}

const DROP_TYPES = [
  dndTypes.CONCEPT_TREE_NODE,
  dndTypes.QUERY_NODE,
  dndTypes.PREVIOUS_QUERY
];

const SxDropzoneWithFileInput = styled(DropzoneWithFileInput)`
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
      margin-top: 70px;
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

const QueryEditorDropzone: React.FC<PropsT> = ({
  isAnd,
  isInitial,
  onLoadPreviousQuery,
  onDropFile,
  onDropNode
}) => {
  const onDrop = (props, monitor) => {
    const item = monitor.getItem();

    if (item.files) {
      onDropFile(item.files[0]);
    } else {
      onDropNode(item);

      if (item.isPreviousQuery) onLoadPreviousQuery(item.id);
    }
  };

  return (
    <SxDropzoneWithFileInput
      isAnd={isAnd}
      isInitial={isInitial}
      acceptedDropTypes={DROP_TYPES}
      onDrop={onDrop}
      onSelectFile={onDropFile}
      disableClick={isInitial}
      showFileSelectButton={isInitial}
    >
      {() => (
        <>
          {isInitial && (
            <TextInitial>
              <h2>{T.translate("dropzone.explanation")}</h2>
              <Row>
                <ArrowRight icon="arrow-right" />
                <div>
                  <p>{T.translate("dropzone.drop")}</p>
                  <ul>
                    <li>{T.translate("dropzone.aConcept")}</li>
                    <li>{T.translate("dropzone.aQuery")}</li>
                    <li>{T.translate("dropzone.aConceptList")}</li>
                  </ul>
                  <p>{T.translate("dropzone.intoThisArea")}</p>
                </div>
              </Row>
            </TextInitial>
          )}
          {!isInitial && (
            <Text>{T.translate("dropzone.dragElementPlease")}</Text>
          )}
        </>
      )}
    </SxDropzoneWithFileInput>
  );
};

export default QueryEditorDropzone;
