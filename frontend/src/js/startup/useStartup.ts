import { useEffect } from "react";
import { useDispatch } from "react-redux";

import { useLoadDatasets } from "../dataset/actions";
import { resetMessage } from "../snack-message/actions";
import { useLoadMe } from "../user/actions";

import { useLoadConfig } from "./actions";

export const useStartup = () => {
  const dispatch = useDispatch();

  const loadConfig = useLoadConfig();
  const loadDatasets = useLoadDatasets();
  const loadMe = useLoadMe();

  useEffect(() => {
    dispatch(resetMessage());
    loadConfig();
    loadDatasets();
    loadMe();
  }, [dispatch]);
};
