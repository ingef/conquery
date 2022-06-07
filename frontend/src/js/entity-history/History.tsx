import styled from "@emotion/styled";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: white;
  padding: 60px 20px 20px;
  z-index: 2;
  display: flex;
  flex-direction: column;
`;

export const History = () => {
  return <FullScreen>Hello history</FullScreen>;
};
