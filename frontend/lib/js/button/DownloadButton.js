// @flow

import * as React from "react";

import { getStoredAuthToken } from "../authorization";

import IconButton from "./IconButton";

type PropsType = {
  url: string,
  className?: string,
  children?: React.Node
};

const DownloadButton = ({
  url,
  className,
  children,
  ...restProps
}: PropsType) => {
  const authToken = getStoredAuthToken();

  const href = `${url}?access_token=${encodeURIComponent(authToken || "")}`;

  return (
    <a href={href} className={className}>
      <IconButton large icon="download" {...restProps}>
        {children}
      </IconButton>
    </a>
  );
};

export default DownloadButton;
