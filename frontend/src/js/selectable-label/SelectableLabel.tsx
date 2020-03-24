import React from "react";
import { connect } from "react-redux";
import { css } from "@emotion/core";
import styled from "@emotion/styled";

type PropsType = {
  className?: string,
  label: string,
  isSelected: boolean
};

const Label = styled("span")`
  ${({ theme, isSelected }) =>
    isSelected &&
    css`
      background-color: ${theme.col.grayVeryLight};
      border: 1px solid ${theme.col.blueGrayLight};
      border-radius: ${theme.borderRadius};
      padding: 0 3px;
    `};
`;

const SelectableLabel = (props: PropsType) => {
  return (
    <Label className={props.className} isSelected={props.isSelected}>
      {props.label}
    </Label>
  );
};

const labelContainsAnySearch = (label, searches) => {
  return searches.some(
    search => label.toLowerCase().indexOf(search.toLowerCase()) !== -1
  );
};

const mapStateToProps = (state, ownProps) => ({
  isSelected: labelContainsAnySearch(
    ownProps.label,
    state.previousQueriesSearch
  )
});

export default connect(mapStateToProps)(SelectableLabel);
