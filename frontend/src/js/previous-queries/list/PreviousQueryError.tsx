import React from "react";

type PropsType = {
  query: {
    error: string;
  };
};

const PreviousQueryError = (props: PropsType) => {
  return (
    !!props.query.error && (
      <div className="previous-query-error">{props.query.error}</div>
    )
  );
};

export default PreviousQueryError;
