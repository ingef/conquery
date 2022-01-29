import { useLocation } from "react-router-dom";

export const useIsCacheEnabled = () => {
  const location = useLocation();

  // Store the token from the URL if it is present.
  const { search } = location;
  const params = new URLSearchParams(search);
  const noCache = params.get("noCache");

  return !noCache || noCache !== "1";
};
