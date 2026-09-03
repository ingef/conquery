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
import { Button, type ButtonProps } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

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

export function getFileIcon(url: string): FileIcon {
  if (url.includes(".")) {
    const ext = getEnding(url);

    if (ext in fileTypeToFileIcon) {
      return fileTypeToFileIcon[ext];
    }
  }

  return { icon: faFileDownload };
}

interface Props extends Omit<ButtonProps, "children" | "className"> {
  resultUrl: ResultUrlWithLabel;
  className?: string;
  children?: ReactNode;
  simpleIcon?: boolean;
  showColoredIcon?: boolean;
}

const DownloadButton = ({
  ref,
  simpleIcon,
  resultUrl,
  className,
  children,
  showColoredIcon,
  ...restProps
}: Props & { ref?: Ref<HTMLAnchorElement> }) => {
  const { authToken } = useContext(AuthTokenContext);

  const href = `${resultUrl.url}?access_token=${encodeURIComponent(authToken)}`;

  const { icon, color } = getFileIcon(resultUrl.url);

  return (
    <a href={href} className={link({ className })} ref={ref}>
      <Button intent="tertiary" className="whitespace-nowrap" {...restProps}>
        <Icon
          icon={simpleIcon ? faDownload : icon}
          style={{ color: showColoredIcon ? color : undefined }}
        />
        {children}
      </Button>
    </a>
  );
};

export default DownloadButton;
