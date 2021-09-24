import styled from "@emotion/styled";
import React, { ReactNode, FC, useContext } from "react";

import { AuthTokenContext } from "../authorization/AuthTokenProvider";

import IconButton, { IconButtonPropsT } from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

interface PropsT extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  url: string;
  className?: string;
  children?: ReactNode;
}

const DownloadButton: FC<PropsT> = ({
  url,
  className,
  children,
  ...restProps
}) => {
  const { authToken } = useContext(AuthTokenContext);

  const href = `${url}?access_token=${encodeURIComponent(
    authToken,
  )}&charset=ISO_8859_1`;

  const icon = "download";

  return (
    <a href={href} className={className}>
      <SxIconButton {...restProps} icon={icon} onClick={() => {}}>
        {children}
      </SxIconButton>
    </a>
  );
};

export default DownloadButton;
