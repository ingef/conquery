// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import DownloadButton from "../button/DownloadButton";

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

type PropsType = {
  resultCount: number,
  resultUrl: string
};

const QueryResults = (props: PropsType) => {
  if (
    typeof props.resultCount === "undefined" &&
    typeof props.resultUrl === "undefined"
  )
    return null;

  let isDownload = props.resultCount > 0 || props.resultUrl;

  return (
    <Root>
      <Text>
        {T.translate("queryRunner.resultCount", { count: props.resultCount })}
      </Text>
      {isDownload && (
        <StyledDownloadButton frame primary url={props.resultUrl}>
          {T.translate("queryRunner.downloadResults")}
        </StyledDownloadButton>
      )}
    </Root>
  );
};

export default QueryResults;
