import { useMemo } from "react";
import { useLocation } from "react-router-dom";

export const useIsCacheEnabled = () => {
  const location = useLocation();

  const { search } = location;
  const params = new URLSearchParams(search);
  const cache = params.get("cache");

  return useMemo(() => !!cache && cache === "1", [cache]);
};
