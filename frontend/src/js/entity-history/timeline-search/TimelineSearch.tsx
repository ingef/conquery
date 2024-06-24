import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDebounce } from "../../common/helpers/useDebounce";
import BaseInput from "../../ui-components/BaseInput";
import { useTimelineSearch } from "./timelineSearchState";

export const TimelineSearch = ({ matches }: { matches: number }) => {
  const { searchVisible, searchTerm, setSearchTerm } = useTimelineSearch();
  const [term, setTerm] = useState(searchTerm || "");
  const { t } = useTranslation();
  useDebounce(() => setSearchTerm(term), 500, [term]);

  if (!searchVisible) return null;

  return (
    <div className="w-full flex flex-col pl-3 pr-5 py-3 gap-1">
      <BaseInput
        inputType="text"
        placeholder={t("history.search")}
        value={term}
        onChange={(value) => setTerm(value as string)}
        className="w-full"
      />
      {searchTerm && (
        <span className="text-xs text-gray-500">
          {matches} {t("history.matches", { count: matches })}
        </span>
      )}
    </div>
  );
};
