import styled from "@emotion/styled";
import { memo, useEffect, useMemo, useState } from "react";
import { ResultUrlsWithLabel } from "../api/types";

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

// Skidding makes Dropdown align the right edge with the button,
// might need to adjust this when adding more content.
const dropdownOffset: [number, number] = [-37, 8]; // [skidding, distance] / default [0, 10]

const getEnding = (url: string) => url.split(".").reverse()[0].toUpperCase();

const getInitialEndingChoice = (resultUrls: ResultUrlsWithLabel[]) => {
  const { preferredDownloadFormat } = getUserSettings();

  const found = resultUrls.find(
    (url) => url.label === preferredDownloadFormat,
  );

  return found ? found.label : resultUrls[0].label;
};

const DownloadResultsDropdownButton = ({
  resultUrls,
  tiny,
  tooltip,
}: {
  resultUrls: ResultUrlsWithLabel[];
  tiny?: boolean;
  tooltip?: string;
}) => {
  const [endingChoice, setEndingChoice] = useState(
    getInitialEndingChoice(resultUrls),
  );

  useEffect(() => {
    storeUserSettings({ preferredDownloadFormat: endingChoice });
  }, [endingChoice]);

  const urlChoice = useMemo(
    () =>
      resultUrls.find((url) => url.label === endingChoice) ||
      resultUrls[0],
    [resultUrls, endingChoice],
  );

  const dropdown = useMemo(() => {
    return (
      <List>
        {resultUrls.map((url) => {
          const ending = getEnding(url.url);

          return (
            <SxDownloadButton
              key={url.url}
              url={url}
              onClick={() => setEndingChoice(ending)}
              bgHover
            >
              {url.label}
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
          <SxDownloadButton bgHover url={urlChoice}>
            {urlChoice.label}
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
          offset={dropdownOffset}
        >
          <SxIconButton bgHover icon={tiny ? "download" : "caret-down"} />
        </WithTooltip>
      </WithTooltip>
    </Frame>
  );
};

export default memo(DownloadResultsDropdownButton);
