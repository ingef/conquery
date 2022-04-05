const DB_NAME = "cache-db";
const OBJECT_STORE_NAME = "dataStore";
const VERSION = 1;
const indexedDB =
  window.indexedDB ||
  (window as any).mozIndexedDB ||
  (window as any).webkitIndexedDB ||
  (window as any).msIndexedDB ||
  (window as any).shimIndexedDB;

function openIndexedDB({
  dbName,
  objectStoreName,
}: {
  dbName: string;
  objectStoreName: string;
}) {
  const openDBrequest = indexedDB.open(dbName, VERSION);

  return new Promise<IDBDatabase>((resolve, reject) => {
    openDBrequest.onupgradeneeded = () =>
      openDBrequest.result.createObjectStore(objectStoreName, {
        keyPath: null,
      });

    openDBrequest.onsuccess = () => resolve(openDBrequest.result);
    openDBrequest.onerror = (evt) => reject(evt);
  });
}

const DB = openIndexedDB({
  dbName: DB_NAME,
  objectStoreName: OBJECT_STORE_NAME,
});

export const clearIndexedDBCache = async () => {
  const db = await DB;

  return new Promise<void>((resolve, reject) => {
    const transaction = db.transaction([OBJECT_STORE_NAME], "readwrite");
    const req = transaction.objectStore(OBJECT_STORE_NAME).clear();

    // The following sometimes got stuck, so avoid for now.
    // const req = indexedDB.deleteDatabase(DB_NAME);

    req.onsuccess = () => resolve();
    req.onerror = () => reject();
  });
};

// Check README for more info on the strategy here
export const getIndexedDBCache = async <T>(key: string) => {
  const db = await DB;

  // Useful for debugging
  // console.debug("IDB GET START: ", key);

  const promise = new Promise<T>((resolve, reject) => {
    const transaction = db.transaction([OBJECT_STORE_NAME], "readwrite");
    const request = transaction.objectStore(OBJECT_STORE_NAME).get(key);

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject();
  });

  const result = await promise;

  // Useful for debugging
  // if (result) {
  // console.debug("IDB HIT: ", key);
  // } else {
  // console.debug("IDB MISS: ", key);
  // }

  return result;
};

export const setIndexedDBCache = async <T>(key: string, value: T) => {
  const db = await DB;

  // Useful for debugging
  // console.debug("IDB SET START", key);
  const promise = new Promise<void>((resolve, reject) => {
    const transaction = db.transaction([OBJECT_STORE_NAME], "readwrite");

    transaction.objectStore(OBJECT_STORE_NAME).put(value, key);
    transaction.oncomplete = () => resolve();
    transaction.onerror = (e) => reject(e);
  });

  await promise;
  // Useful for debugging
  // console.debug("IDB SET COMPLETE", key);
};
