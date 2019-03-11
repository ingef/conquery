import React from "react";
import T from "i18n-react";

import PreviousQueriesFilterButton from "./PreviousQueriesFilterButton";

const PreviousQueriesFilter = () => {
  return (
    <div className="previous-queries-filter">
      <PreviousQueriesFilterButton
        value="all"
        text={T.translate("previousQueriesFilter.all")}
      />
      <span className="previous-queries-filter__spacer" />
      <PreviousQueriesFilterButton
        value="own"
        text={T.translate("previousQueriesFilter.own")}
      />
      <PreviousQueriesFilterButton
        value="system"
        text={T.translate("previousQueriesFilter.system")}
      />
      <PreviousQueriesFilterButton
        value="shared"
        text={T.translate("previousQueriesFilter.shared")}
      />
    </div>
  );
};

export default PreviousQueriesFilter;
