import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faDownload,
  faFileArchive,
  faFileCode,
  faFileCsv,
  faFileDownload,
  faFileExcel,
  faFilePdf,
} from "@fortawesome/free-solid-svg-icons";
import { type ReactNode, type Ref, useContext } from "react";
import { tv } from "tailwind-variants";
import type { ResultUrlWithLabel } from "../api/types";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { getEnding } from "../query-runner/DownloadResultsDropdownButton";

import IconButton, { type IconButtonPropsT } from "./IconButton";

const link = tv({ base: "leading-none" });

interface FileIcon {
  icon: IconProp;
  color?: string;
}

const fileTypeToFileIcon: Record<string, FileIcon> = {
  ZIP: { icon: faFileArchive, color: "var(--color-filetype-zip)" },
  XLSX: { icon: faFileExcel, color: "var(--color-filetype-xlsx)" },
  PDF: { icon: faFilePdf, color: "var(--color-filetype-pdf)" },
  CSV: { icon: faFileCsv, color: "var(--color-filetype-csv)" },
  JSON: { icon: faFileCode, color: "var(--color-filetype-json)" },
};

function getFileIcon(url: string): FileIcon {
  if (url.includes(".")) {
    const ext = getEnding(url);

    if (ext in fileTypeToFileIcon) {
      return fileTypeToFileIcon[ext];
    }
  }

  return { icon: faFileDownload };
}

interface Props extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  resultUrl: ResultUrlWithLabel;
  className?: string;
  children?: ReactNode;
  simpleIcon?: boolean;
  onClick?: () => void;
  showColoredIcon?: boolean;
}

const DownloadButton = ({
  ref,
  simpleIcon,
  resultUrl,
  className,
  children,
  onClick,
  showColoredIcon,
  ...restProps
}: Props & { ref?: Ref<HTMLAnchorElement> }) => {
  const { authToken } = useContext(AuthTokenContext);

  const href = `${resultUrl.url}?access_token=${encodeURIComponent(authToken)}`;

  const { icon, color } = getFileIcon(resultUrl.url);

  return (
    <a href={href} className={link({ className })} ref={ref}>
      <IconButton
        {...restProps}
        className="whitespace-nowrap"
        large
        icon={simpleIcon ? faDownload : icon}
        onClick={onClick}
        iconColor={showColoredIcon ? color : undefined}
      >
        {children}
      </IconButton>
    </a>
  );
};

export default DownloadButton;
