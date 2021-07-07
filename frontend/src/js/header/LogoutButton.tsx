import styled from "@emotion/styled";
import { useKeycloak } from "@react-keycloak/web";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { deleteStoredAuthToken } from "../authorization/helper";
import IconButton from "../button/IconButton";
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
  const history = useHistory();
  const { keycloak } = useKeycloak();
  const goToLogin = () => history.push("/login");

  const onLogout = () => {
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
