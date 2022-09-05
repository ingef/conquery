import styled from "@emotion/styled";
import { ReactNode, useContext, forwardRef } from "react";

import { AuthTokenContext } from "../authorization/AuthTokenProvider";

import IconButton, { IconButtonPropsT } from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

const Link = styled("a")`
  line-height: 1;
`;

interface Props extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  url: string;
  className?: string;
  children?: ReactNode;
}

const DownloadButton = forwardRef<HTMLAnchorElement, Props>(
  ({ url, className, children, ...restProps }, ref) => {
    const { authToken } = useContext(AuthTokenContext);

    const href = `${url}?access_token=${encodeURIComponent(
      authToken,
    )}&charset=ISO_8859_1`;

    const icon = "download";

    return (
      <Link href={href} className={className} ref={ref}>
        <SxIconButton {...restProps} icon={icon} onClick={() => {}}>
          {children}
        </SxIconButton>
      </Link>
    );
  },
);

export default DownloadButton;
