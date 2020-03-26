import React from "react";
import T from "i18n-react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import DatasetSelector from "../dataset/DatasetSelector";

const Root = styled("header")`
  background-color: ${({ theme }) => theme.col.graySuperLight};
  color: ${({ theme }) => theme.col.blueGrayDark};
  border-bottom: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.3);
  padding: 0 20px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;

  // Fix, so content can expand to 100% and scroll
  position: absolute;
  z-index: 3;
  width: 100%;
  top: 0;
  left: 0;
`;

const OverflowHidden = styled("div")`
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const Spacer = styled("span")`
  margin: 0 10px;
  height: 20px;
`;

const Logo = styled("div")`
  height: 50px;
  width: ${({ theme }) => theme.img.logoWidth};
  background-image: url(${({ theme }) => theme.img.logo});
  background-repeat: no-repeat;
  background-position-y: 50%;
  background-size: ${({ theme }) => theme.img.logoBackgroundSize};
`;

const Headline = styled("h1")`
  margin: 0 auto 0 0;
  line-height: 2;
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 300;
`;

type PropsType = {
  version: string;
  isDevelopment: boolean;
};

// TODO: Show version somewhere
class Header extends React.Component<PropsType> {
  render() {
    return (
      <Root>
        <OverflowHidden>
          <Logo title={this.props.version} />
          <Spacer />
          <Headline>{T.translate("headline")}</Headline>
        </OverflowHidden>
        <DatasetSelector />
      </Root>
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  return {
    version: state.startup.config.version,
    isDevelopment: !state.startup.config.production || false
  };
};

export default connect(mapStateToProps, {})(Header);
