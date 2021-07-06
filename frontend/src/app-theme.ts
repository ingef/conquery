import type { Theme } from "@emotion/react";
import styled from "@emotion/styled";

import logo from "./images/conquery-logo.png";
import spinner from "./images/spinner.png";

export const theme: Theme = {
  col: {
    bg: "#fafafa",
    bgAlt: "#f3f6f4",
    black: "#222",
    gray: "#888",
    grayMediumLight: "#aaa",
    grayLight: "#dadada",
    grayVeryLight: "#eee",
    blueGrayDark: "#0C6427",
    blueGray: "#72757C",
    blueGrayLight: "#52A55C",
    blueGrayVeryLight: "#A4E6AC",
    red: "#b22125",
    green: "#36971C",
    orange: "#E9711C",
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
