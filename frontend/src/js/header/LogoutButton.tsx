import React, { FC } from "react";
import styled from "@emotion/styled";
import IconButton from "../button/IconButton";
import { deleteStoredAuthToken } from "../authorization/helper";
import WithTooltip from "../tooltip/WithTooltip";
import { T } from "../localization";
import { useHistory } from "react-router-dom";
import { isIDPEnabled } from "js/environment";
import { useKeycloak } from "@react-keycloak/web";

const SxIconButton = styled(IconButton)`
  padding: 10px 6px;
`;

interface PropsT {
  className?: string;
}

const LogoutButton: FC<PropsT> = ({ className }) => {
  const history = useHistory();
  const { keycloak } = useKeycloak();
  const goToLogin = () => history.push("/login");

  const onLogout = () => {
    deleteStoredAuthToken();

    if (isIDPEnabled()) {
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
    <WithTooltip className={className} text={T.translate("common.logout")}>
      <SxIconButton frame icon="sign-out-alt" onClick={onLogout} />
    </WithTooltip>
  );
};

export default LogoutButton;
