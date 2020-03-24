import React from "react";
import { connect } from "react-redux";
import styled from "@emotion/styled";
import T from "i18n-react";

import DownloadButton from "../button/DownloadButton";
import PreviewButton from "../button/PreviewButton";
import FaIcon from "../icon/FaIcon";
import { isEmpty } from "../common/helpers/commonHelper";
import { canDownloadResult } from "../user/selectors";

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

type PropsType = {
  datasetId: string;
  resultCount: number;
  resultUrl: string;
  userCanDownloadResult: Boolean;
};

const QueryResults = (props: PropsType) => {
  const isDownloadAllowed = !!props.resultUrl && props.userCanDownloadResult;
  const ending = isDownloadAllowed
    ? props.resultUrl.split(".").reverse()[0]
    : null;

  return (
    <Root>
      {isEmpty(props.resultCount) ? (
        <Text>
          <FaIcon icon="check" left />
          {T.translate("queryRunner.endSuccess")}
        </Text>
      ) : (
        <LgText>
          <Bold>{props.resultCount}</Bold>{" "}
          {T.translate("queryRunner.resultCount")}
        </LgText>
      )}
      {ending === "csv" && <SxPreviewButton url={props.resultUrl} />}
      {isDownloadAllowed && ending && (
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

export default connect((state, ownProps) => ({
  userCanDownloadResult: canDownloadResult(state, ownProps.datasetId)
}))(QueryResults);
