import React, { FC } from "react";
import { useSelector } from "react-redux";
import styled from "@emotion/styled";
import T from "i18n-react";

import DownloadButton from "../button/DownloadButton";
import PreviewButton from "../button/PreviewButton";
import FaIcon from "../icon/FaIcon";
import { isEmpty } from "../common/helpers/commonHelper";
import { canDownloadResult } from "../user/selectors";
import type { ColumnDescription } from "js/api/types";
import type { StateT } from "app-types";

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
  datasetId: string;
  resultCount: number;
  resultUrl: string;
  resultColumns: ColumnDescription[];
}

const QueryResults: FC<PropsT> = ({
  datasetId,
  resultUrl,
  resultCount,
  resultColumns,
}) => {
  const userCanDownloadResult = useSelector<StateT, boolean>((state) =>
    canDownloadResult(state, datasetId)
  );

  const isDownloadAllowed = !!resultUrl && userCanDownloadResult;
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
          <Bold>{resultCount}</Bold> {T.translate("queryRunner.resultCount")}
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
