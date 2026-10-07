import { parseISO } from "date-fns";
import { useEffect, useRef } from "react";
import { useDateFormatter } from "react-aria";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { NewsItemT } from "../api/types";
import { useIntersectionObserver } from "../common/useIntersectionObserver";
import { Link } from "../ui-components/Link";
import { C2, C3, H4 } from "../ui-components/Typography";
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

const root = tv({
  base: [
    "relative",
    "flex flex-col items-start",
    "gap-1",
    "px-5 py-4",
    "whitespace-pre-line",
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
      <div className="relative flex">
        <span className={unreadMarker({ isUnread })}>
          <UnreadDot />
        </span>
        <C3 as="span" tone="muted">
          <time dateTime={date}>{dateFormatter.format(parseISO(date))}</time>
        </C3>
        {isUnread && <span className="sr-only">{t("news.unread")}</span>}
      </div>
      <H4>{title}</H4>
      <C2>{description}</C2>
      {href && (
        <Link href={href} external>
          {t("news.more")}
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
