import * as React from "react";
import { useSelector, useDispatch } from "react-redux";
import T from "i18n-react";

import { getStoredAuthToken } from "../authorization/helper";
import { openPreview } from "../preview/actions";

import IconButton from "./IconButton";
import { StateT } from "app-types";

type PropsType = {
  url: string;
  className?: string;
};

const PreviewButton = ({ url, className, ...restProps }: PropsType) => {
  const authToken = getStoredAuthToken();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading
  );

  const dispatch = useDispatch();
  const onOpenPreview = (url: string) => dispatch(openPreview(url));

  const href = `${url}?access_token=${encodeURIComponent(
    authToken || ""
  )}&charset=utf-8`;

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

export default PreviewButton;
