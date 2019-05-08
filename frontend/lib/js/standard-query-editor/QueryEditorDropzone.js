// @flow

import React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import T from "i18n-react";
import { NativeTypes } from "react-dnd-html5-backend";

import Dropzone from "../form-components/Dropzone";
import FaIcon from "../icon/FaIcon";

import { dndTypes } from "../common/constants";
import type { QueryIdType } from "../common/types/backend";
import type { DraggedNodeType, DraggedQueryType } from "./types";

type DraggedFileType = Object;

type PropsType = {
  isInitial?: boolean,
  isAnd?: boolean,
  onDropNode: (DraggedNodeType | DraggedQueryType) => void,
  onDropFile: DraggedFileType => void,
  onLoadPreviousQuery: QueryIdType => void
};

const DROP_TYPES = [
  dndTypes.CATEGORY_TREE_NODE,
  dndTypes.QUERY_NODE,
  dndTypes.PREVIOUS_QUERY,
  NativeTypes.FILE
];

const StyledDropzone = styled(Dropzone)`
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

const QueryEditorDropzone = ({
  isAnd,
  isInitial,
  onLoadPreviousQuery,
  onDropFile,
  onDropNode
}: PropsType) => {
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
    <StyledDropzone
      isAnd={isAnd}
      isInitial={isInitial}
      acceptedDropTypes={DROP_TYPES}
      onDrop={onDrop}
    >
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
      {!isInitial && <Text>{T.translate("dropzone.dragElementPlease")}</Text>}
    </StyledDropzone>
  );
};

export default QueryEditorDropzone;
