// @flow

import React, { useEffect } from "react";
import { useDispatch } from "react-redux";

import { startup, loadConfig } from "./actions";
import { startup as userStartup } from "../user/actions";

export const useStartup = () => {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(loadConfig());
    dispatch(startup());
    dispatch(userStartup());
  }, []);
};
