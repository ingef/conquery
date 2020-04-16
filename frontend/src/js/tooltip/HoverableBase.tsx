import React from "react";
import styled from "@emotion/styled";

const Root = styled("div")`
  cursor: pointer;
`;

type PropsType = {
  className?: string;
  onDisplayAdditionalInfos: Function;
  onToggleAdditionalInfos: Function;
};

const HoverableBase = (Component: any) =>
  class extends React.Component {
    props: PropsType;

    render() {
      return (
        <Root
          className={this.props.className}
          onMouseEnter={this.props.onDisplayAdditionalInfos}
          onClick={this.props.onToggleAdditionalInfos}
        >
          <Component {...this.props} />
        </Root>
      );
    }
  };

export default HoverableBase;
