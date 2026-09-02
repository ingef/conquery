import { faSignOutAlt } from "@fortawesome/free-solid-svg-icons";
import { useKeycloak } from "@react-keycloak-fork/web";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";

import { deleteStoredAuthToken } from "../authorization/helper";
import IconButton from "../button/IconButton";
import { clearIndexedDBCache } from "../common/helpers/indexedDBCache";
import { isIDPEnabled } from "../environment";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const LogoutButton = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const goToLogin = () => navigate("/login");

  const onLogout = async () => {
    await clearIndexedDBCache();

    deleteStoredAuthToken();

    if (isIDPEnabled) {
      keycloak.logout();
    } else {
      goToLogin();

      // Hard refresh to reset all state
      // and reload all data
      const ARBITRARY_SHORT_TIME = 200;
      setTimeout(() => {
        window.location.reload();
      }, ARBITRARY_SHORT_TIME);
    }
  };

  return (
    <TooltipTrigger>
      <IconButton small frame icon={faSignOutAlt} onClick={onLogout} />
      <Tooltip>{t("common.logout")}</Tooltip>
    </TooltipTrigger>
  );
};

export default LogoutButton;
