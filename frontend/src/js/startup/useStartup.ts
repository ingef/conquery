import { useEffect } from "react";
import { useDispatch } from "react-redux";

import { useLoadConfig } from "./actions";
import { resetMessage } from "../snack-message/actions";
import { useLoadDatasets } from "../dataset/actions";
import { useLoadMe } from "../user/actions";

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
