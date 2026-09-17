import { parseISO } from "date-fns";
import { ExternalLinkIcon } from "lucide-react";
import { useRef } from "react";
import { useDateFormatter } from "react-aria";
import { Link } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { NewsItemT } from "../api/types";
import { useIntersectionObserver } from "../common/useIntersectionObserver";
import { UnreadDot } from "../ui-components/UnreadDot";

const list = tv({
  base: [
    "flex flex-col",
    "max-h-[min(70vh,480px)]",
    "overflow-y-auto",
    "divide-y divide-gray-100",
  ],
});

// explicit: through the portal the text would inherit body's line-height and light weight
const root = tv({
  base: [
    "relative",
    "flex flex-col",
    "gap-1",
    "px-5 py-4",
    "text-sm leading-5 font-normal",
    "text-gray-800",
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

const NewsListItem = ({
  item: { id, title, description, link: href, date },
  isUnread,
  onSeen,
}: {
  item: NewsItemT;
  isUnread: boolean;
  onSeen: (id: NewsItemT["id"]) => void;
}) => {
  const { t } = useTranslation();
  const dateFormatter = useDateFormatter({ dateStyle: "long" });

  // seen = the item's end has been scrolled into view
  const endRef = useRef<HTMLSpanElement>(null);
  useIntersectionObserver(endRef, (_, isIntersecting) => {
    if (isIntersecting) onSeen(id);
  });

  return (
    <li className={root()}>
      <p className="flex items-center gap-2 text-xs text-gray-500">
        {isUnread && <UnreadDot />}
        <time dateTime={date}>{dateFormatter.format(parseISO(date))}</time>
        {isUnread && <span className="sr-only">{t("news.unread")}</span>}
      </p>
      <h4 className="font-bold">{title}</h4>
      <p className="whitespace-pre-line">{description}</p>
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
      <span ref={endRef} className="absolute bottom-1 left-0 size-px" />
    </li>
  );
};

export const NewsList = ({
  items,
  unreadIds,
  onSeen,
}: {
  items: NewsItemT[];
  unreadIds: Set<NewsItemT["id"]>;
  onSeen: (id: NewsItemT["id"]) => void;
}) => (
  <ul className={list()}>
    {items.map((item) => (
      <NewsListItem
        key={item.id}
        item={item}
        isUnread={unreadIds.has(item.id)}
        onSeen={onSeen}
      />
    ))}
  </ul>
);
