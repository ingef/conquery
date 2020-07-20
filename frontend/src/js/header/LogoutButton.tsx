import React, { FC } from "react";
import styled from "@emotion/styled";
import IconButton from "../button/IconButton";
import { deleteStoredAuthToken } from "../authorization/helper";
import WithTooltip from "../tooltip/WithTooltip";
import { T } from "../localization";

const SxWithTooltip = styled(WithTooltip)`
  margin-left: 5px;
`;

const SxIconButton = styled(IconButton)`
  padding: 10px 6px;
`;

interface PropsT {
  className?: string;
}

const LogoutButton: FC<PropsT> = () => {
  const onLogout = () => {
    deleteStoredAuthToken();

    // Hard refresh to reset all state
    // and reload all data
    window.location.href = "/login";
  };

  return (
    <SxWithTooltip text={T.translate("common.logout")}>
      <SxIconButton frame icon="sign-out-alt" onClick={onLogout} />
    </SxWithTooltip>
  );
};

export default LogoutButton;
