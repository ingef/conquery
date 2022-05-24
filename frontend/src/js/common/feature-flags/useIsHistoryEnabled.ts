import { useLocation } from "react-router-dom";

export const useIsHistoryEnabled = () => {
  const location = useLocation();

  // Store the token from the URL if it is present.
  const { search } = location;
  const params = new URLSearchParams(search);
  const history = params.get("history");

  return !!history && history === "1";
};
