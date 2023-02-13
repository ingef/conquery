import styled from "@emotion/styled";
import { ReactNode, useContext, forwardRef } from "react";
import { ResultUrlsWithLabel } from "../api/types";
import { IconName } from "@fortawesome/fontawesome-svg-core";

import { AuthTokenContext } from "../authorization/AuthTokenProvider";

import IconButton, { IconButtonPropsT } from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

const Link = styled("a")`
  line-height: 1;
`;

const fileTypeToIcon: Record<string, IconName> = {
  "ZIP": "file-archive",
  "XLSX": "file-excel",
  "PDF": "file-pdf",
  "CSV": "file-csv",
}
function getFileIcon(label:string): IconName {
  // Editor Requests
  if(label in fileTypeToIcon) {
    return fileTypeToIcon[label];
  }

  // Forms
  if(label.includes(".")){
    const ext = label.split(".").pop()?.toLocaleUpperCase();
    console.log("ext " + ext)
    if(!ext) return "file-download";
    if(ext in fileTypeToIcon){
       return fileTypeToIcon[ext];
    }
  }
  return "file-download";
}

interface Props extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  url: ResultUrlsWithLabel;
  className?: string;
  children?: ReactNode;
  onClick?: () => void;
}

const DownloadButton = forwardRef<HTMLAnchorElement, Props>(
  ({ url, className, children, onClick, ...restProps }, ref) => {
    const { authToken } = useContext(AuthTokenContext);

    const href = `${url.url}?access_token=${encodeURIComponent(
      authToken,
    )}&charset=ISO_8859_1`;

    return (
      <Link href={href} className={className} ref={ref}>
        <SxIconButton {...restProps} icon={getFileIcon(url.label)} onClick={onClick}>
          {children}
        </SxIconButton>
      </Link>
    );
  },
);

export default DownloadButton;
