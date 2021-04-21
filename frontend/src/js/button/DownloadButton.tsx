import React, { ReactNode, FC } from "react";

import { useAuthToken } from "../api/useApi";

import IconButton from "./IconButton";

interface PropsT {
  url: string;
  className?: string;
  children?: ReactNode;
  ending: string;
}

function getIcon(ending: string) {
  return "download";

  // TODO: RE-Enable this with better icons (maybe "regular style" when we can afford it)
  // switch (ending) {
  //   case "csv":
  //     return "file-csv";
  //   case "zip":
  //     return "file-archive";
  //   default:
  //     return "file-alt";
  // }
}

const DownloadButton: FC<PropsT> = ({
  url,
  className,
  children,
  ending,
  ...restProps
}) => {
  const authToken = useAuthToken();

  const href = `${url}?access_token=${encodeURIComponent(
    authToken,
  )}&charset=ISO_8859_1`;

  const icon = getIcon(ending);

  return (
    <a href={href} className={className}>
      <IconButton icon={icon} {...restProps}>
        {children}
      </IconButton>
    </a>
  );
};

export default DownloadButton;
