import { faSearch } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "../../ui-components/Icon";
import { ToggleButton } from "../../ui-components/ToggleButton";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { useTimelineSearch } from "./timelineSearchState";

const SearchControl = () => {
  const { t } = useTranslation();

  const { searchVisible, setSearchVisible } = useTimelineSearch();

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <ToggleButton
          aria-label={t("history.search")}
          isSelected={searchVisible}
          onChange={setSearchVisible}
        >
          <Icon icon={faSearch} />
        </ToggleButton>
        <Tooltip placement="right">{t("history.search")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(SearchControl);
