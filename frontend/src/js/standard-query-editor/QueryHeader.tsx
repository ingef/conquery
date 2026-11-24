import { memo } from "react";

import QueryClearButton from "./QueryClearButton";

const QueryHeader = () => {
  return (
    <div className="mb-[5px] flex items-center justify-end">
      <QueryClearButton />
    </div>
  );
};

export default memo(QueryHeader);
