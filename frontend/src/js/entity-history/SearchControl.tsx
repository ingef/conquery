import { memo } from "react";
import { useTranslation } from "react-i18next";

import { faSearch } from "@fortawesome/free-solid-svg-icons";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import { useTimelineSearch } from "./timelineSearchState";

const SearchControl = () => {
  const { t } = useTranslation();

  const { searchVisible, setSearchVisible } = useTimelineSearch();
  const toggleSearchVisible = () => setSearchVisible(!searchVisible);

  return (
    <div className="flex flex-col items-center">
      <WithTooltip text={t("history.search")}>
        <IconButton
          className="px-[10px] py-2"
          active={searchVisible}
          onClick={toggleSearchVisible}
          icon={faSearch}
        />
      </WithTooltip>
    </div>
  );
};

export default memo(SearchControl);
