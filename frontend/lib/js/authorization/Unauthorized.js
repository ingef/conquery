// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

const Unauthorized = styled("div")`
  text-align: center;
  margin: 50px;
  font-size: 30px;
`;

export default () => (
  <Unauthorized>{T.translate("authorization.unauthorized")}</Unauthorized>
);
