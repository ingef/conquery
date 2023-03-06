import styled from "@emotion/styled";
import { memo, useEffect, useMemo, useState } from "react";

import { ResultUrlWithLabel } from "../api/types";
import DownloadButton from "../button/DownloadButton";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

const Frame = styled("div")<{ noborder?: boolean }>`
  display: flex;
  align-items: center;
  justify-content: center;
  border: ${({ noborder, theme }) =>
    noborder ? "none" : `1px solid ${theme.col.gray}`};
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: opacity ${({ theme }) => theme.transitionTime};
`;

const List = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 8px;
`;

const SxDownloadButton = styled(DownloadButton)`
  button {
    width: 100%;
    padding: 8px 14px;
  }
`;
const SxIconButton = styled(IconButton)`
  padding: 9px 8px;
`;

const Separator = styled("div")`
  width: 1px;
  height: 33px;
  background-color: ${({ theme }) => theme.col.gray};
`;

const popperOptions = {
  modifiers: [
    {
      name: "preventOverflow",
      options: {
        padding: 20,
      },
    },
  ],
};

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

  const dropdown = useMemo(() => {
    return (
      <List>
        {resultUrls.map((resultUrl) => {
          const ending = getEnding(resultUrl.url);

          return (
            <SxDownloadButton
              key={resultUrl.url}
              resultUrl={resultUrl}
              onClick={() => setFileChoice({ label: resultUrl.label, ending })}
              bgHover
            >
              {truncate(resultUrl.label)}
            </SxDownloadButton>
          );
        })}
      </List>
    );
  }, [resultUrls]);

  return (
    <Frame noborder={tiny}>
      {!tiny && (
        <>
          <SxDownloadButton bgHover resultUrl={urlChoice}>
            {truncChosenLabel}
          </SxDownloadButton>
          <Separator />
        </>
      )}
      <WithTooltip text={tooltip} hideOnClick>
        <WithTooltip
          html={dropdown}
          interactive
          arrow={false}
          trigger="click"
          popperOptions={popperOptions}
        >
          <SxIconButton bgHover icon={tiny ? "download" : "caret-down"} />
        </WithTooltip>
      </WithTooltip>
    </Frame>
  );
};

export default memo(DownloadResultsDropdownButton);
