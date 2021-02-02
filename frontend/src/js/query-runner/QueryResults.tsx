import React, { FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import DownloadButton from "../button/DownloadButton";
import PreviewButton from "../button/PreviewButton";
import FaIcon from "../icon/FaIcon";
import { isEmpty } from "../common/helpers/commonHelper";
import type { ColumnDescription } from "../api/types";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const Text = styled("p")`
  margin: 0 10px 0 0;
  line-height: 1;
  font-size: ${({ theme }) => theme.font.sm};
`;

const LgText = styled(Text)`
  font-size: ${({ theme }) => theme.font.lg};
`;

const StyledDownloadButton = styled(DownloadButton)`
  display: inline-block;
`;

const SxPreviewButton = styled(PreviewButton)`
  margin-right: 10px;
`;

const Bold = styled("span")`
  font-weight: 700;
`;

interface PropsT {
  resultCount: number;
  resultUrl: string;
  resultColumns: ColumnDescription[];
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
}

const QueryResults: FC<PropsT> = ({
  resultUrl,
  resultCount,
  resultColumns,
  queryType,
}) => {
  const isDownloadAllowed = !!resultUrl;
  const ending = isDownloadAllowed ? resultUrl.split(".").reverse()[0] : null;

  return (
    <Root>
      {isEmpty(resultCount) ? (
        <Text>
          <FaIcon icon="check" left />
          {T.translate("queryRunner.endSuccess")}
        </Text>
      ) : (
        <LgText>
          <Bold>{resultCount}</Bold>{" "}
          {queryType === "SECONDARY_ID_QUERY"
            ? T.translate("queryRunner.resultCountSecondaryIdQuery")
            : T.translate("queryRunner.resultCount")}
        </LgText>
      )}
      {ending === "csv" && (
        <SxPreviewButton columns={resultColumns} url={resultUrl} />
      )}
      {isDownloadAllowed && ending && (
        <StyledDownloadButton frame primary ending={ending} url={resultUrl}>
          {ending.toUpperCase()}
        </StyledDownloadButton>
      )}
    </Root>
  );
};

export default QueryResults;
