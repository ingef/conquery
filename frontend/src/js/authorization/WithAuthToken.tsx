import { useKeycloak } from "@react-keycloak/web";
import { FC, useContext } from "react";
import { useHistory } from "react-router-dom";

import { isLoginDisabled, isIDPEnabled } from "../environment";

import { AuthTokenContext } from "./AuthTokenProvider";

const WithAuthToken: FC = ({ children }) => {
  const { initialized } = useKeycloak();
  const history = useHistory();
  const { authToken } = useContext(AuthTokenContext);
  const goToLogin = () => history.push("/login");

  if (isIDPEnabled && (!initialized || !authToken)) {
    return null;
  }

  if (!isIDPEnabled && !isLoginDisabled && !authToken) {
    goToLogin();
    return null;
  }

  return <>{children}</>;
};

export default WithAuthToken;
