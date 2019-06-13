import theme from "conquery/app/src/styles/theme";

import spinner from "../images/spinner-eva.png";
import logo from "../images/EVA_Logo_normal_RGB.svg";

export default {
  ...theme,
  col: {
    ...theme.col,
    bgAlt: "#f3f5f6",
    blueGrayDark: "#324f5f",
    blueGray: "#98a7af",
    blueGrayLight: "#ccd3d7",
    blueGrayVeryLight: "#ccd3d7"
  },
  img: {
    ...theme.img,
    logo: logo,
    logoWidth: "90px",
    logoBackgroundSize: "90px 90px",
    spinner: spinner
  }
};
