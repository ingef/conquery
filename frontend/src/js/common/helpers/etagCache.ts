import { getIndexedDBCache, setIndexedDBCache } from "./indexedDBCache";

type Etag = string;
interface CachedResourceWithEtag {
  etag: Etag;
  resource: Object;
}

export const storeEtagResource = (
  key: string,
  etag: string,
  resource: Object,
) => {
  const item = { etag, resource };

  return setIndexedDBCache(key, item);
};

export const getCachedEtagResource = async (
  key: string,
): Promise<CachedResourceWithEtag | null> => {
  const cachedItem = await getIndexedDBCache<CachedResourceWithEtag>(key);

  return cachedItem || null;
};
