import React, { ReactNode, FC } from "react";

import { useAuthToken } from "../api/useApi";

import IconButton, { IconButtonPropsT } from "./IconButton";

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
  const authToken = useAuthToken();

  const href = `${url}?access_token=${encodeURIComponent(
    authToken,
  )}&charset=ISO_8859_1`;

  const icon = "download";

  return (
    <a href={href} className={className}>
      <IconButton {...restProps} icon={icon} onClick={() => {}}>
        {children}
      </IconButton>
    </a>
  );
};

export default DownloadButton;
