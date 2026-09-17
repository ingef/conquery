import { useEffect, useState } from "react";

import { useIsCacheEnabled } from "../common/feature-flags/useIsCacheEnabled";
import { clearIndexedDBCache } from "../common/helpers/indexedDBCache";
import Header from "../header/Header";
import { SnackMessage } from "../snack-message/SnackMessage";
import { useStartup } from "../startup/useStartup";
import { About } from "./About";
import Content from "./Content";

const useCacheClear = () => {
  const [cacheReady, setCacheReady] = useState<boolean>(false);
  const cacheEnabled = useIsCacheEnabled();

  useEffect(() => {
    async function maybeClearCache() {
      if (!cacheEnabled && !cacheReady) {
        await clearIndexedDBCache();
        setCacheReady(true);
      } else {
        setCacheReady(true);
      }
    }

    maybeClearCache();
  }, [cacheEnabled, cacheReady]);

  return cacheReady;
};

const App = () => {
  const cacheReady = useCacheClear();

  useStartup({ ready: cacheReady });

  return (
    <div className="relative h-screen w-full">
      <About />
      <Header />
      <Content />
      <SnackMessage />
    </div>
  );
};

export default App;
