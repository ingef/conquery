import { useEffect } from "react";
import { useDispatch } from "react-redux";

import { useLoadDatasets } from "../dataset/actions";
import { resetMessage } from "../snack-message/actions";
import { useLoadMe } from "../user/actions";

import { useLoadConfig } from "./actions";

export const useStartup = ({ ready }: { ready?: boolean }) => {
  const dispatch = useDispatch();

  const loadConfig = useLoadConfig();
  const loadDatasets = useLoadDatasets();
  const loadMe = useLoadMe();

  useEffect(() => {
    async function load() {
      if (ready) {
        dispatch(resetMessage());
        await loadMe();
        await loadConfig();
        setTimeout(loadDatasets, 300);
      }
    }
    load();
  }, [dispatch, ready, loadConfig, loadDatasets, loadMe]);
};
