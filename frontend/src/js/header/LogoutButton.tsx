import React, { FC } from "react";
import styled from "@emotion/styled";
import IconButton from "../button/IconButton";
import { deleteStoredAuthToken } from "../authorization/helper";
import WithTooltip from "../tooltip/WithTooltip";
import { T } from "../localization";

const SxIconButton = styled(IconButton)`
  padding: 10px 6px;
`;

interface PropsT {
  className?: string;
}

const LogoutButton: FC<PropsT> = ({ className }) => {
  const onLogout = () => {
    deleteStoredAuthToken();

    // Hard refresh to reset all state
    // and reload all data
    window.location.href = "/login";
  };

  return (
    <WithTooltip className={className} text={T.translate("common.logout")}>
      <SxIconButton frame icon="sign-out-alt" onClick={onLogout} />
    </WithTooltip>
  );
};

export default LogoutButton;
