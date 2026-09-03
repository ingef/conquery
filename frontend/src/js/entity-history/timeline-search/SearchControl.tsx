import { faSearch } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../../ui-components/Button";
import { Icon } from "../../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { useTimelineSearch } from "./timelineSearchState";

const SearchControl = () => {
  const { t } = useTranslation();

  const { searchVisible, setSearchVisible } = useTimelineSearch();
  const toggleSearchVisible = () => setSearchVisible(!searchVisible);

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <Button
          aria-label={t("history.search")}
          intent="tertiary"
          aria-pressed={searchVisible}
          onPress={toggleSearchVisible}
        >
          <Icon icon={faSearch} />
        </Button>
        <Tooltip placement="right">{t("history.search")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(SearchControl);
