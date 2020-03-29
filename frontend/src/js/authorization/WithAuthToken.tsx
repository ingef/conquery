import React from "react";
import { storeAuthToken, getStoredAuthToken } from "./helper";
import { push } from "react-router-redux";
import { isLoginDisabled } from "../environment";
import { useDispatch } from "react-redux";

type PropsType = {
  children: React.ReactNode;
  location: {
    search: Object;
  };
};

const WithAuthToken = ({ location, children }: PropsType) => {
  const dispatch = useDispatch();
  const goToLogin = () => dispatch(push("/login"));

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
