import { MegaphoneIcon } from "lucide-react";
import { Dialog, DialogTrigger, Heading } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Dot } from "../ui-components/Dot";
import { Popover } from "../ui-components/Popover";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { NewsList } from "./NewsList";
import { useNews } from "./useNews";

const unreadMarker = tv({
  base: [
    "absolute -top-[3px] -right-[3px]",
    "rounded-full",
    "ring-2 ring-bg-50",
    "pointer-events-none",
  ],
});

// explicit: a RAC Heading renders an h3, which has base styles
const headline = tv({
  base: [
    "m-0",
    "px-5 py-3",
    "border-b border-gray-100",
    "text-sm leading-5 font-bold",
    "text-gray-800",
  ],
});

export const NewsButton = () => {
  const { t } = useTranslation();
  const { items, unreadIds, markAllRead } = useNews();

  if (items.length === 0) return null;

  const label =
    unreadIds.size > 0
      ? `${t("news.headline")}, ${t("news.unreadCount", { count: unreadIds.size })}`
      : t("news.headline");

  return (
    <TooltipTrigger>
      <DialogTrigger
        onOpenChange={(isOpen) => {
          if (!isOpen) markAllRead();
        }}
      >
        <span className="relative flex">
          <Button intent="secondary" aria-label={label} data-test-id="news">
            <MegaphoneIcon />
          </Button>
          {unreadIds.size > 0 && (
            <span className={unreadMarker()}>
              <Dot />
            </span>
          )}
        </span>
        <Popover placement="bottom end" className="w-[400px]">
          <Dialog className="outline-none">
            <Heading slot="title" className={headline()}>
              {t("news.headline")}
            </Heading>
            <NewsList items={items} unreadIds={unreadIds} />
          </Dialog>
        </Popover>
      </DialogTrigger>
      <Tooltip>{t("news.headline")}</Tooltip>
    </TooltipTrigger>
  );
};
