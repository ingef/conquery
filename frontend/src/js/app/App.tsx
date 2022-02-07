import styled from "@emotion/styled";
import { clearCache } from "mustache";
import { useEffect, useState } from "react";

import { clearIndexedDBCache } from "../common/helpers/indexedDBCache";
import { useIsCacheEnabled } from "../common/useIsCacheEnabled";
import Header from "../header/Header";
import SnackMessage from "../snack-message/SnackMessage";
import { useStartup } from "../startup/useStartup";

import Content, { ContentPropsT } from "./Content";

const Root = styled("div")`
  height: 100vh;
  width: 100%;
  position: relative;
`;

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

const App = (props: ContentPropsT) => {
  const cacheReady = useCacheClear();

  useStartup({ ready: cacheReady });

  return (
    <Root>
      <Header />
      <Content {...props} />
      <SnackMessage />
    </Root>
  );
};

export default App;
