import { faSearch } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import IconButton from "../../button/IconButton";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { useTimelineSearch } from "./timelineSearchState";

const SearchControl = () => {
  const { t } = useTranslation();

  const { searchVisible, setSearchVisible } = useTimelineSearch();
  const toggleSearchVisible = () => setSearchVisible(!searchVisible);

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <IconButton
          className="px-[10px] py-2"
          active={searchVisible}
          onClick={toggleSearchVisible}
          icon={faSearch}
        />
        <Tooltip placement="right">{t("history.search")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(SearchControl);
