import { useLocation } from "react-router-dom";

export const useIsHistoryEnabled = () => {
  const location = useLocation();

  const { search } = location;
  const params = new URLSearchParams(search);
  const history = params.get("history");

  return !!history && history === "1";
};
