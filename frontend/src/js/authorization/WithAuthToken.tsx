import { useKeycloak } from "@react-keycloak/web";
import { FC, useContext } from "react";
import { useNavigate } from "react-router-dom";

import { isLoginDisabled, isIDPEnabled } from "../environment";

import { AuthTokenContext } from "./AuthTokenProvider";

const WithAuthToken: FC = ({ children }) => {
  const { initialized } = useKeycloak();
  const navigate = useNavigate();
  const { authToken } = useContext(AuthTokenContext);

  const goToLogin = () => navigate("/login");

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
