import { useKeycloak } from "@react-keycloak-fork/web";
import { ReactNode, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { isIDPEnabled, isLoginDisabled } from "../environment";

import { AuthTokenContext } from "./AuthTokenProvider";

const WithAuthToken = ({ children }: { children: ReactNode }) => {
  const { initialized } = useKeycloak();
  const navigate = useNavigate();
  const { authToken } = useContext(AuthTokenContext);

  const [goToLogin, setGoToLogin] = useState(false);

  useEffect(
    function asyncGoToLogin() {
      // Has to be async, because navigate as returned from useNavigate()
      // can't be called on the first component render
      if (goToLogin) {
        navigate("/login");
      }
    },
    [goToLogin, navigate],
  );

  if (isIDPEnabled && (!initialized || !authToken)) {
    return null;
  }

  if (!isIDPEnabled && !isLoginDisabled && !authToken) {
    if (!goToLogin) {
      setGoToLogin(true);
    }
    return null;
  }

  return <>{children}</>;
};

export default WithAuthToken;
