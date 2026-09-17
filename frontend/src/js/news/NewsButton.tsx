import { MegaphoneIcon } from "lucide-react";
import { Dialog, DialogTrigger, Heading } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Popover } from "../ui-components/Popover";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { UnreadDot } from "../ui-components/UnreadDot";
import { NewsList } from "./NewsList";
import { useNews } from "./useNews";

// the ring cuts the dot out of the icon, in the button's background
const unreadMarker = tv({
  base: [
    "absolute top-[3px] right-[3px]",
    "rounded-full",
    "ring-2 ring-bg-50 group-hover:ring-gray-50",
    "pointer-events-none",
  ],
});

// explicit: a RAC Heading renders an h3, which has base styles
const headline = tv({
  base: [
    "flex items-center",
    "gap-2",
    "m-0",
    "px-5 py-3",
    "border-b border-gray-100",
    "text-sm leading-5 font-medium",
    "text-gray-800",
  ],
});

export const NewsButton = () => {
  const { t } = useTranslation();
  const { items, unreadIds, markRead } = useNews();

  if (items.length === 0) return null;

  const label =
    unreadIds.size > 0
      ? `${t("news.headline")}, ${t("news.unreadCount", { count: unreadIds.size })}`
      : t("news.headline");

  return (
    <TooltipTrigger>
      <DialogTrigger>
        <span className="group relative flex">
          <Button intent="secondary" aria-label={label} data-test-id="news">
            <MegaphoneIcon />
          </Button>
          {unreadIds.size > 0 && (
            <span className={unreadMarker()}>
              <UnreadDot />
            </span>
          )}
        </span>
        <Popover placement="bottom end" className="w-[400px]">
          <Dialog className="outline-none">
            <Heading slot="title" className={headline()}>
              <MegaphoneIcon />
              {t("news.headline")}
            </Heading>
            <NewsList items={items} unreadIds={unreadIds} onRead={markRead} />
          </Dialog>
        </Popover>
      </DialogTrigger>
      <Tooltip>{t("news.headline")}</Tooltip>
    </TooltipTrigger>
  );
};
