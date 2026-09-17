import { differenceInCalendarDays, parseISO } from "date-fns";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { useGetNews } from "../api/api";
import type { NewsItemT } from "../api/types";
import type { StateT } from "../app/reducers";
import { useDatasetId } from "../dataset/selectors";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

// older items never count as unread, so a fresh browser doesn't flag the whole archive
const UNREAD_MAX_AGE_DAYS = 60;
const MAX_STORED_IDS = 200;

const getReadIds = (userName: string) =>
  getUserSettings().readNewsIds?.[userName] ?? [];

const storeReadIds = (userName: string, ids: string[]) =>
  storeUserSettings({
    readNewsIds: {
      ...getUserSettings().readNewsIds,
      [userName]: ids.slice(-MAX_STORED_IDS),
    },
  });

const useNewsItems = () => {
  const datasetId = useDatasetId();
  const getNews = useGetNews();
  const [items, setItems] = useState<NewsItemT[]>([]);

  useEffect(() => {
    let isCurrent = true;

    async function loadNews() {
      if (!datasetId) return;

      try {
        const news = await getNews(datasetId);

        if (isCurrent) setItems(news);
      } catch {
        if (isCurrent) setItems([]);
      }
    }

    loadNews();

    return () => {
      isCurrent = false;
    };
  }, [datasetId, getNews]);

  return useMemo(
    () => [...items].sort((a, b) => b.date.localeCompare(a.date)),
    [items],
  );
};

export const useNews = () => {
  const items = useNewsItems();
  const userName = useSelector<StateT, string>(
    (state) => state.user.me?.userName ?? "",
  );
  const [readIds, setReadIds] = useState<string[]>([]);

  useEffect(() => setReadIds(getReadIds(userName)), [userName]);

  const unreadIds = useMemo(() => {
    const today = new Date();

    return new Set(
      items
        .filter(
          ({ id, date }) =>
            !readIds.includes(id) &&
            differenceInCalendarDays(today, parseISO(date)) <=
              UNREAD_MAX_AGE_DAYS,
        )
        .map(({ id }) => id),
    );
  }, [items, readIds]);

  const markAllRead = useCallback(() => {
    if (unreadIds.size === 0) return;

    const nextReadIds = [...readIds, ...unreadIds];

    storeReadIds(userName, nextReadIds);
    setReadIds(nextReadIds);
  }, [readIds, unreadIds, userName]);

  return { items, unreadIds, markAllRead };
};
