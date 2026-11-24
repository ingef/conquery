import { useKeycloak } from "@react-keycloak-fork/web";
import { useNavigate } from "react-router";
import { deleteStoredAuthToken } from "../authorization/helper";
import { clearIndexedDBCache } from "../common/helpers/indexedDBCache";
import { isIDPEnabled } from "../environment";

export const useLogout = () => {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();

  return async () => {
    await clearIndexedDBCache();

    deleteStoredAuthToken();

    if (isIDPEnabled) {
      keycloak.logout();
    } else {
      navigate("/login");

      // Hard refresh to reset all state
      // and reload all data
      const ARBITRARY_SHORT_TIME = 200;
      setTimeout(() => {
        window.location.reload();
      }, ARBITRARY_SHORT_TIME);
    }
  };
};
