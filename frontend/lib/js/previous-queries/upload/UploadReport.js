// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { type UploadReportType } from "./reducer";

type PropsType = {
  report: UploadReportType
};

const Root = styled("div")`
  margin: 0 auto 10px;
  display: inline-block;
`;

const SuccessfulCount = styled("p")`
  margin: 0;
  text-align: left;
  font-size: ${({ theme }) => theme.font.sm};
`;

const UnsuccessfulCount = styled(SuccessfulCount)`
  color: ${({ theme }) => theme.col.red};
`;

const UploadReport = (props: PropsType) => (
  <Root>
    <SuccessfulCount>
      {T.translate("uploadReport.successful", {
        count: props.report.successful
      })}
    </SuccessfulCount>
    {props.report.unsuccessful > 0 && (
      <UnsuccessfulCount>
        {T.translate("uploadReport.unsuccessful", {
          count: props.report.unsuccessful
        })}
      </UnsuccessfulCount>
    )}
  </Root>
);

export default UploadReport;
