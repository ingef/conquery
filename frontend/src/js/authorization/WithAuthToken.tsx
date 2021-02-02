import React, { FC } from "react";
import { storeAuthToken, getStoredAuthToken } from "./helper";
import { isLoginDisabled } from "../environment";
import { useHistory } from "react-router-dom";

interface PropsT {
  location: {
    search: Object;
  };
}

const WithAuthToken: FC<PropsT> = ({ location, children }) => {
  const history = useHistory();
  const goToLogin = () => history.push("/login");

  const { search } = location;
  const params = new URLSearchParams(search);
  const accessToken = params.get("access_token");

  if (accessToken) storeAuthToken(accessToken);

  if (!isLoginDisabled()) {
    const authToken = getStoredAuthToken();

    if (!authToken) {
      goToLogin();
      return null;
    }
  }

  return <>{children}</>;
};

export default WithAuthToken;
