import { css, Global } from "@emotion/react";

const globalStyles = css`
  * {
    box-sizing: border-box;
  }

  p {
    line-height: 1.5;
  }

  body {
    font-family: "Roboto", "Arial", sans-serif;
    font-weight: 300;
    background: $col-bg;
    margin: 0;
    color: $col-black;
    min-height: 100vh;
    height: 100%;
    overflow: hidden;
  }

  button,
  select,
  input {
    font-family: "Roboto", "Arial", sans-serif;
  }

  a,
  a:visited,
  a:active {
    color: $col-black;
    text-decoration: none;
  }

  button {
    outline: none;
    cursor: pointer;
  }
  button::-moz-focus-inner {
    padding: 0;
    border: 0;
  }

  input::-ms-clear {
    display: none;
  }

  select {
    outline: 0;
    border-radius: 3px;
    padding: 8px 30px 8px 8px;
    border: 1px solid $col-gray;
    appearance: none;
    font-size: $font-sm;
    cursor: pointer;
    &:disabled {
      cursor: not-allowed;
    }
    &:hover {
      background-color: $col-gray-very-light;
    }
  }

  h3 {
    font-size: $font-sm;
    margin: 0 0 10px 0;
    color: $col-blue-gray-dark;
    line-height: 25px;
  }
`;

const splitPaneStyles = css`
  .SplitPane {
    .Pane1 {
      overflow: hidden;
    }

    .Pane2 {
      overflow: hidden;
    }

    .SplitPane--tooltip-fixed {
      > .Pane1 {
        width: 30px !important; // Because SplitPane sets an element style after the first drag
      }
    }
  }

  .Resizer {
    background: $col-gray-medium-light;
    opacity: 0.8;
    z-index: 1;
    transition: all 0.5s ease-in-out;
    box-sizing: border-box;
    -moz-background-clip: padding;
    -webkit-background-clip: padding;
    background-clip: padding-box;

    &.vertical {
      width: 11px;
      margin: 0 -5px;
      border-left: 5px solid rgba(0, 0, 0, 0);
      border-right: 5px solid rgba(0, 0, 0, 0);
      cursor: col-resize;

      &:hover {
        &:not(.disabled) {
          border-left: 5px solid rgba(0, 0, 0, 0.1);
          border-right: 5px solid rgba(0, 0, 0, 0.1);
        }
      }
    }

    &.horizontal {
      height: 11px;
      margin: -5px 0;
      border-top: 5px solid rgba(0, 0, 0, 0);
      border-bottom: 5px solid rgba(0, 0, 0, 0);
      cursor: row-resize;

      &:hover {
        &:not(.disabled) {
          border-top: 5px solid rgba(0, 0, 0, 0.1);
          border-bottom: 5px solid rgba(0, 0, 0, 0.1);
        }
      }
    }

    &.disabled {
      cursor: not-allowed;
    }
  }
`;

const GlobalStyles = () => (
  <>
    <Global styles={globalStyles} />
    <Global styles={splitPaneStyles} />
  </>
);

export default GlobalStyles;
