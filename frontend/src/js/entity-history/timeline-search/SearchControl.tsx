import { faSearch } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "../../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { SidebarToggle } from "../SidebarControl";
import { useTimelineSearch } from "./timelineSearchState";

const SearchControl = () => {
  const { t } = useTranslation();

  const { searchVisible, setSearchVisible } = useTimelineSearch();

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <SidebarToggle
          aria-label={t("history.search")}
          isSelected={searchVisible}
          onChange={setSearchVisible}
        >
          <Icon icon={faSearch} />
        </SidebarToggle>
        <Tooltip placement="right">{t("history.search")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(SearchControl);
