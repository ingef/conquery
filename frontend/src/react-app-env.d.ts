/// <reference types="react-scripts" />

import "@emotion/react";

declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: "development" | "production";
    REACT_APP_API_URL?: string;
    REACT_APP_DISABLE_LOGIN?: boolean;
    REACT_APP_LANG?: "de" | "en";
    PORT?: string;
  }
}

declare module "@emotion/react" {
  export interface Theme {
    col: {
      bg: string;
      bgAlt: string;
      black: string;
      gray: string;
      grayMediumLight: string;
      grayLight: string;
      grayVeryLight: string;
      graySuperLight: string;
      red: string;
      blueGrayDark: string;
      blueGray: string;
      blueGrayLight: string;
      blueGrayVeryLight: string;
      green: string;
      orange: string;
    };
    img: {
      logo: string;
      logoWidth: string;
      logoBackgroundSize: string;
      spinner: string;
    };
    font: {
      huge: string;
      lg: string;
      md: string;
      sm: string;
      xs: string;
      tiny: string;
    };
    maxWidth: string;
    borderRadius: string;
    transitionTime: string;
  }
}

declare global {
  interface Window {
    datasetId: string | null;
  }
}
