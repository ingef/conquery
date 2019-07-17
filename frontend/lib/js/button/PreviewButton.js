// @flow

import * as React from "react";
import { connect } from "react-redux";
import T from "i18n-react";

import { getStoredAuthToken } from "../authorization";
import { openPreview } from "../preview/actions";

import IconButton from "./IconButton";

type PropsType = {
  url: string,
  onOpenPreview: (url: string) => void,
  className?: string
};

const PreviewButton = ({
  url,
  onOpenPreview,
  className,
  ...restProps
}: PropsType) => {
  const authToken = getStoredAuthToken();

  const href = `${url}?access_token=${encodeURIComponent(authToken || "")}`;

  return (
    <IconButton
      icon="search"
      onClick={() => onOpenPreview(href)}
      {...restProps}
    >
      {T.translate("preview.preview")}
    </IconButton>
  );
};

export default connect(
  null,
  dispatch => ({
    onOpenPreview: (url: string) => dispatch(openPreview(url))
  })
)(PreviewButton);
