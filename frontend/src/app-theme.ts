import type { Theme } from "@emotion/react";
import styled from "@emotion/styled";

import logo from "./images/conquery-logo.png";
import spinner from "./images/spinner.png";

export const theme: Theme = {
  col: {
    bg: "#fafafa",
    black: "#222",
    gray: "#888",
    grayMediumLight: "#aaa",
    grayLight: "#dadada",
    grayVeryLight: "#eee",
    red: "#b22125",
    green: "#36971C",
    orange: "#E9711C",
    palette: [
      "#f9c74f",
      "#f8961e",
      "#277da1",
      "#90be6d",
      "#43aa8b",
      "#f94144",
      "#5e60ce",
      "#aaa",
      "#777",
      "#fff",
    ],
    bgAlt: "#f4f6f5",
    blueGrayDark: "#1f5f30",
    blueGray: "#98b099",
    blueGrayLight: "#ccd6d0",
    blueGrayVeryLight: "#dadedb",
  },
  img: {
    logo: logo,
    logoWidth: "172px",
    logoBackgroundSize: "172px 40px",
    spinner: spinner,
  },
  font: {
    huge: "24px",
    lg: "20px",
    md: "16px",
    sm: "14px",
    xs: "12px",
    tiny: "11px",
  },
  maxWidth: "1024px",
  borderRadius: "3px",
  transitionTime: "0.1s",
};

export default styled;
