import React, { FC } from "react";

import IconButton from "../button/IconButton";

import { clearQuery } from "./actions";
import { useDispatch } from "react-redux";

interface PropsT {
  className?: string;
}

const QueryClearButton: FC<PropsT> = ({ className }) => {
  const dispatch = useDispatch();
  const onClearQuery = () => dispatch(clearQuery());

  return (
    <div className={className}>
      <IconButton frame onClick={onClearQuery} regular icon="trash-alt" />
    </div>
  );
};

export default QueryClearButton;
