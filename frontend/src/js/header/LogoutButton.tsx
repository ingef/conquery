import styled from "@emotion/styled";
import { useKeycloak } from "@react-keycloak/web";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { deleteStoredAuthToken } from "../authorization/helper";
import IconButton from "../button/IconButton";
import { clearIndexedDBCache } from "../common/helpers/indexedDBCache";
import { isIDPEnabled } from "../environment";
import WithTooltip from "../tooltip/WithTooltip";

const SxIconButton = styled(IconButton)`
  padding: 6px 6px;
`;

interface PropsT {
  className?: string;
}

const LogoutButton: FC<PropsT> = ({ className }) => {
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
    <WithTooltip className={className} text={t("common.logout")}>
      <SxIconButton frame icon="sign-out-alt" onClick={onLogout} />
    </WithTooltip>
  );
};

export default LogoutButton;
