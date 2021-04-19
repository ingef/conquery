import styled from "@emotion/styled";
import React from "react";

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
