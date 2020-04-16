import * as React from "react";
import { connect } from "react-redux";
import T from "i18n-react";

import { getStoredAuthToken } from "../authorization/helper";
import { openPreview } from "../preview/actions";

import IconButton from "./IconButton";

type PropsType = {
  url: string,
  isLoading: boolean,
  onOpenPreview: (url: string) => void,
  className?: string
};

const PreviewButton = ({
  url,
  isLoading,
  onOpenPreview,
  className,
  ...restProps
}: PropsType) => {
  const authToken = getStoredAuthToken();

  const href = `${url}?access_token=${encodeURIComponent(authToken || "")}`;

  return (
    <IconButton
      icon={isLoading ? "spinner" : "search"}
      onClick={() => onOpenPreview(href)}
      {...restProps}
    >
      {T.translate("preview.preview")}
    </IconButton>
  );
};

export default connect(
  state => ({
    isLoading: state.preview.isLoading
  }),
  dispatch => ({
    onOpenPreview: (url: string) => dispatch(openPreview(url))
  })
)(PreviewButton);
