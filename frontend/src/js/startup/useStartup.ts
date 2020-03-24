import { useEffect } from "react";
import { useDispatch } from "react-redux";

import { startup, loadConfig } from "./actions";
import { startup as userStartup } from "../user/actions";
import { resetMessage } from "../snack-message/actions";

export const useStartup = () => {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(resetMessage());
    dispatch(loadConfig());
    dispatch(startup());
    dispatch(userStartup());
  }, [dispatch]);
};
