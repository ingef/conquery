import { parseISO } from "date-fns";
import { ExternalLinkIcon } from "lucide-react";
import { useDateFormatter } from "react-aria";
import { Link } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { NewsItemT } from "../api/types";
import { Dot } from "../ui-components/Dot";

const list = tv({
  base: [
    "flex flex-col",
    "max-h-[min(70vh,480px)]",
    "overflow-y-auto",
    "divide-y divide-gray-100",
  ],
});

// explicit: through the portal the text would inherit body's line-height and light weight
const item = tv({
  base: [
    "flex flex-col",
    "gap-1",
    "px-5 py-4",
    "text-sm leading-5 font-normal",
  ],
});

const link = tv({
  base: [
    "flex items-center self-start",
    "gap-1",
    "rounded-sm",
    "font-medium",
    "text-primary-500",
    "cursor-pointer",
    "outline-none",
    "hover:underline",
    "data-focus-visible:underline",
  ],
});

export const NewsList = ({
  items,
  unreadIds,
}: {
  items: NewsItemT[];
  unreadIds: Set<string>;
}) => {
  const { t } = useTranslation();
  const dateFormatter = useDateFormatter({ dateStyle: "long" });

  return (
    <ul className={list()}>
      {items.map(({ id, title, description, link: href, date }) => (
        <li key={id} className={item()}>
          <p className="flex items-center gap-2 text-xs text-gray-500">
            {unreadIds.has(id) && <Dot />}
            <time dateTime={date}>{dateFormatter.format(parseISO(date))}</time>
            {unreadIds.has(id) && (
              <span className="sr-only">{t("news.unread")}</span>
            )}
          </p>
          <h4 className="font-medium text-gray-800">{title}</h4>
          <p className="whitespace-pre-line text-gray-800">{description}</p>
          {href && (
            <Link
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className={link()}
            >
              {t("news.more")}
              <ExternalLinkIcon />
            </Link>
          )}
        </li>
      ))}
    </ul>
  );
};
