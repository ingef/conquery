import { parseISO } from "date-fns";
import { ExternalLinkIcon } from "lucide-react";
import { useEffect, useRef } from "react";
import { useDateFormatter } from "react-aria";
import { Link } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { NewsItemT } from "../api/types";
import { useIntersectionObserver } from "../common/useIntersectionObserver";
import { UnreadDot } from "../ui-components/UnreadDot";

const READ_AFTER_MS = 1500;

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

// in the item's left padding, centered on the date line, so nothing moves when it fades
const unreadMarker = tv({
  base: [
    "absolute top-1/2 -left-[14px]",
    "-translate-y-1/2",
    "transition-opacity duration-500",
  ],
  variants: {
    isUnread: { false: "opacity-0" },
  },
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
  onRead,
}: {
  item: NewsItemT;
  isUnread: boolean;
  onRead: (id: NewsItemT["id"]) => void;
}) => {
  const { t } = useTranslation();
  const dateFormatter = useDateFormatter({ dateStyle: "long" });

  // read = the item's end stayed in view for a moment
  const endRef = useRef<HTMLSpanElement>(null);
  const readTimeout = useRef<ReturnType<typeof setTimeout>>(undefined);

  useIntersectionObserver(endRef, (_, isIntersecting) => {
    clearTimeout(readTimeout.current);

    if (isIntersecting && isUnread) {
      readTimeout.current = setTimeout(() => onRead(id), READ_AFTER_MS);
    }
  });

  useEffect(() => () => clearTimeout(readTimeout.current), []);

  return (
    <li className={root()}>
      <p className="relative text-xs text-gray-500">
        <span className={unreadMarker({ isUnread })}>
          <UnreadDot />
        </span>
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
  onRead,
}: {
  items: NewsItemT[];
  unreadIds: Set<NewsItemT["id"]>;
  onRead: (id: NewsItemT["id"]) => void;
}) => (
  <ul className={list()}>
    {items.map((item) => (
      <NewsListItem
        key={item.id}
        item={item}
        isUnread={unreadIds.has(item.id)}
        onRead={onRead}
      />
    ))}
  </ul>
);
