import { getIndexedDBCache, setIndexedDBCache } from "./indexedDBCache";

type Etag = string;
interface CachedResourceWithEtag {
  etag: Etag;
  resource: object;
}

const encode = (obj: object) => {
  // TODO: Use strings directly, not objs.
  // Since this is used to cache responses, we could use the raw string returned from the API call,
  // before axios calls JSON.parse on it.
  const str = JSON.stringify(obj);
  const encoder = new TextEncoder();

  return encoder.encode(str);
};
const decode = (buf: Uint8Array) => {
  const decoder = new TextDecoder();
  const str = decoder.decode(buf);

  return JSON.parse(str);
};

export const storeEtagResource = (
  key: string,
  etag: string,
  resource: object,
) => {
  const item = { etag, resource };

  return setIndexedDBCache(key, encode(item));
};

export const getCachedEtagResource = async (
  key: string,
): Promise<CachedResourceWithEtag | null> => {
  const cachedItem = await getIndexedDBCache<Uint8Array>(key);

  return cachedItem ? decode(cachedItem) : null;
};
