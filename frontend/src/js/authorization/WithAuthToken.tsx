import { useKeycloak } from "@react-keycloak/web";
import React, { FC } from "react";
import { useHistory } from "react-router-dom";

import { isLoginDisabled, isIDPEnabled } from "../environment";

import { storeAuthToken, getStoredAuthToken } from "./helper";

interface PropsT {
  location: {
    search: Object;
  };
}

const WithAuthToken: FC<PropsT> = ({ location, children }) => {
  const history = useHistory();
  const { keycloak, initialized } = useKeycloak();
  const goToLogin = () => history.push("/login");

  const { search } = location;
  const params = new URLSearchParams(search);
  const accessToken = params.get("access_token");

  if (accessToken) storeAuthToken(accessToken);

  if (isIDPEnabled && (!initialized || !keycloak.token)) {
    return null;
  }

  if (!isIDPEnabled && !isLoginDisabled) {
    const authToken = getStoredAuthToken();

    if (!authToken) {
      goToLogin();
      return null;
    }
  }

  return <>{children}</>;
};

export default WithAuthToken;
