import { ListIcon, SearchIcon } from "lucide-react";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../../app/reducers";
import { C1, H2 } from "../../ui-components/Typography";
import type { EntityHistoryStateT } from "../reducer";

const root = tv({
  base: [
    "flex flex-col items-center justify-center",
    "p-5",
    "w-full",
    "text-gray-500",
  ],
});

const bigIcon = tv({
  base: ["size-10", "text-gray-100"],
});

export const TimelineEmptyPlaceholder = ({
  className,
  searchTerm,
}: {
  className?: string;
  searchTerm?: string;
}) => {
  const { t } = useTranslation();

  const ids = useSelector<StateT, EntityHistoryStateT["entityIds"]>(
    (state) => state.entityHistory.entityIds,
  );
  const id = useSelector<StateT, EntityHistoryStateT["currentEntityId"]>(
    (state) => state.entityHistory.currentEntityId,
  );

  // named messageHtml: the tv const `message` would be shadowed otherwise
  const messageHtml = useMemo(() => {
    if (searchTerm) {
      return t("history.emptyTimeline.descriptionWithSearchTerm", {
        searchTerm,
      });
    }

    if (ids.length === 0 || !id) {
      return t("history.emptyTimeline.descriptionWithoutIds");
    }

    return t("history.emptyTimeline.descriptionWithId");
  }, [ids, id, t, searchTerm]);

  return (
    <div className={root({ className })}>
      <div className="flex items-center gap-[30px]">
        {searchTerm ? (
          <SearchIcon className={bigIcon()} />
        ) : (
          <ListIcon className={bigIcon()} />
        )}
        <div>
          <H2>{t("history.emptyTimeline.headline")}</H2>
          <C1>{t("history.emptyTimeline.description")}</C1>
          <div className="mt-[10px]">
            <C1
              tone="default"
              // biome-ignore lint/security/noDangerouslySetInnerHtml: messageHtml is our own i18n text
              dangerouslySetInnerHTML={{ __html: messageHtml }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
