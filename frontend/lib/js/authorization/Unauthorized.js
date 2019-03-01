// @flow

import React from "react";
import T from "i18n-react";

const Unauthorized = () => (
  <div
    style={{
      textAlign: "center",
      margin: "50px",
      fontSize: "30px"
    }}
  >
    {T.translate("authorization.unauthorized")}
  </div>
);

export default Unauthorized;
