import styled from "@emotion/styled";
import { useState } from "react";

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
  const [cacheClearedOnce, setCacheClearedOnce] = useState<boolean>(false);
  const cacheEnabled = useIsCacheEnabled();

  if (!cacheEnabled && !cacheClearedOnce) {
    clearIndexedDBCache();
    setCacheClearedOnce(true);
  }
};

const App = (props: ContentPropsT) => {
  useCacheClear();
  useStartup();

  return (
    <Root>
      <Header />
      <Content {...props} />
      <SnackMessage />
    </Root>
  );
};

export default App;
