// @flow

import React from "react";
import T from "i18n-react";
import classnames from "classnames";

type PropsType = {
  isStartStopLoading: boolean,
  isQueryRunning: boolean,
  disabled: boolean,
  onClick: Function
};

// A button that is prefixed by an icon
const QueryRunnerButton = ({
  onClick,
  isStartStopLoading,
  isQueryRunning,
  disabled
}: PropsType) => {
  const label = isQueryRunning
    ? T.translate("queryRunner.stop")
    : T.translate("queryRunner.start");

  const iconClassName = classnames({
    "fa-stop": !isStartStopLoading && isQueryRunning,
    "fa-play": !isStartStopLoading && !isQueryRunning,
    "fa-spinner": !!isStartStopLoading
  });

  return (
    <button
      type="button"
      className={classnames("query-runner-button", {
        "query-runner-button--running": isQueryRunning
      })}
      onClick={onClick}
      disabled={disabled}
    >
      <span className="query-runner-button__icon">
        <i className={classnames("fa", iconClassName)} />
      </span>
      <span className="query-runner-button__label">{label}</span>
    </button>
  );
};

export default QueryRunnerButton;
