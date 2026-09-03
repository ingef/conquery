import { faCaretDown, faDownload } from "@fortawesome/free-solid-svg-icons";
import { memo, useContext, useEffect, useMemo, useState } from "react";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { ResultUrlWithLabel } from "../api/types";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import DownloadButton, { getFileIcon } from "../button/DownloadButton";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import { Menu, MenuItem, menuItemIcon } from "../ui-components/Menu";
import { Popover } from "../ui-components/Popover";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

const frame = tv({
  base: [
    "flex items-center justify-center",
    "rounded",
    "border border-gray-500",
    "transition-opacity duration-100",
  ],
  variants: {
    noborder: { true: "border-none" },
  },
});

const downloadButton = tv({
  base: ["[&_button]:w-full", "[&_button]:px-[14px] [&_button]:py-2"],
});

const dropdownOpenButton = tv({ base: "px-2 py-[9px]" });

const separator = tv({ base: ["h-[33px] w-px", "bg-gray-500"] });

interface FileChoice {
  label: string;
  ending: string;
}

export const getEnding = (url: string) =>
  url.split(".").reverse()[0].toUpperCase();

function getResultUrl(
  resultUrls: ResultUrlWithLabel[],
  fileChoice: FileChoice,
): ResultUrlWithLabel {
  return (
    resultUrls.find(({ label }) => label === fileChoice.label) ||
    resultUrls.find(({ url }) => getEnding(url) === fileChoice.ending) ||
    resultUrls[0]
  );
}

function truncate(label: string) {
  return label.length > 40 ? `${label.slice(0, 37)}…` : label;
}

const getInitialEndingChoice = (resultUrls: ResultUrlWithLabel[]) => {
  const { preferredDownloadEnding: ending, preferredDownloadLabel: label } =
    getUserSettings();
  return getResultUrl(resultUrls, { label: label || "", ending: ending || "" });
};

const DownloadResultsDropdownButton = ({
  resultUrls,
  tiny,
  tooltip,
}: {
  resultUrls: ResultUrlWithLabel[];
  tiny?: boolean;
  tooltip?: string;
}) => {
  const { t } = useTranslation();
  const { authToken } = useContext(AuthTokenContext);
  const [fileChoice, setFileChoice] = useState<FileChoice>(() => {
    const initial = getInitialEndingChoice(resultUrls);
    return { label: initial.label, ending: getEnding(initial.url) };
  });

  useEffect(() => {
    storeUserSettings({
      preferredDownloadEnding: fileChoice.ending,
      preferredDownloadLabel: fileChoice.label,
    });
  }, [fileChoice]);

  const urlChoice = useMemo(() => {
    return getResultUrl(resultUrls, fileChoice);
  }, [resultUrls, fileChoice]);

  const truncChosenLabel = useMemo(() => {
    return truncate(fileChoice.label);
  }, [fileChoice]);

  return (
    <div className={frame({ noborder: tiny })}>
      {!tiny && (
        <>
          <DownloadButton
            className={downloadButton()}
            bgHover
            resultUrl={urlChoice}
            showColoredIcon
          >
            {truncChosenLabel}
          </DownloadButton>
          <div className={separator()} />
        </>
      )}
      <TooltipTrigger>
        <MenuTrigger>
          <IconButton
            className={dropdownOpenButton()}
            bgHover
            icon={tiny ? faDownload : faCaretDown}
          />
          <Popover containerPadding={20}>
            <Menu
              aria-label={t("previousQuery.downloadResults")}
              onAction={(key) => {
                const chosen = resultUrls.find(({ url }) => url === key);
                if (chosen) {
                  setFileChoice({
                    label: chosen.label,
                    ending: getEnding(chosen.url),
                  });
                }
              }}
            >
              {resultUrls.map((resultUrl) => {
                const { icon, color } = getFileIcon(resultUrl.url);

                return (
                  <MenuItem
                    key={resultUrl.url}
                    id={resultUrl.url}
                    href={`${resultUrl.url}?access_token=${encodeURIComponent(authToken)}`}
                    textValue={resultUrl.label}
                  >
                    <span className={menuItemIcon()}>
                      <FaIcon large icon={icon} style={{ color }} />
                    </span>
                    {truncate(resultUrl.label)}
                  </MenuItem>
                );
              })}
            </Menu>
          </Popover>
        </MenuTrigger>
        <Tooltip>{tooltip}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(DownloadResultsDropdownButton);
