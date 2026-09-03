import { faListUl, faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../../app/reducers";
import { Icon } from "../../ui-components/Icon";
import type { EntityHistoryStateT } from "../reducer";

const root = tv({
  base: [
    "flex flex-col items-center justify-center",
    "p-5",
    "w-full",
    "font-normal",
    "text-gray-500",
  ],
});

const message = tv({
  base: ["mt-[10px]", "text-xl", "font-normal", "text-gray-800"],
});

const bigIcon = tv({
  base: ["text-[120px]", "text-gray-100"],
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
        <Icon
          icon={searchTerm ? faMagnifyingGlass : faListUl}
          className={bigIcon()}
        />
        <div>
          <h2 className="text-2xl leading-[1.3]">
            {t("history.emptyTimeline.headline")}
          </h2>
          <p className="text-xl">{t("history.emptyTimeline.description")}</p>
          <p
            className={message()}
            // biome-ignore lint/security/noDangerouslySetInnerHtml: messageHtml is our own i18n text
            dangerouslySetInnerHTML={{ __html: messageHtml }}
          />
        </div>
      </div>
    </div>
  );
};
