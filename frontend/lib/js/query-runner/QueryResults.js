// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import DownloadButton from "../button/DownloadButton";
import PreviewButton from "../button/PreviewButton";
import { isEmpty } from "../common/helpers/commonHelper";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const Text = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 0 10px 0 0;
  line-height: 1;
`;

const StyledDownloadButton = styled(DownloadButton)`
  display: inline-block;
`;

const SxPreviewButton = styled(PreviewButton)`
  margin-right: 10px;
`;

type PropsType = {
  resultCount: number,
  resultUrl: string
};

const QueryResults = (props: PropsType) => {
  if (isEmpty(props.resultUrl)) return null;

  const isDownload = !!props.resultUrl;
  const ending = props.resultUrl.split(".").reverse()[0];

  return (
    <Root>
      {!isEmpty(props.resultCount) && (
        <Text>
          {T.translate("queryRunner.resultCount", { count: props.resultCount })}
        </Text>
      )}
      {ending === "csv" && <SxPreviewButton url={props.resultUrl} />}
      {isDownload && (
        <StyledDownloadButton
          frame
          primary
          ending={ending}
          url={props.resultUrl}
        >
          {ending.toUpperCase()}
        </StyledDownloadButton>
      )}
    </Root>
  );
};

export default QueryResults;
