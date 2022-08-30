import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import DownloadButton from "../button/DownloadButton";
import WithTooltip from "../tooltip/WithTooltip";

interface Props {
  className?: string;
}

export const DownloadEntityDataButton = ({ className }: Props) => {
  const { t } = useTranslation();
  const csvUrl = useSelector<StateT, string>(
    (state) => state.entityHistory.currentEntityCsvUrl,
  );

  return (
    <WithTooltip text={t("history.downloadEntityData")}>
      <DownloadButton className={className} url={csvUrl}></DownloadButton>
    </WithTooltip>
  );
};
