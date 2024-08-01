import styled from "@emotion/styled";
import { faPlay, faSpinner, faStop } from "@fortawesome/free-solid-svg-icons";
import { forwardRef } from "react";
import { useTranslation } from "react-i18next";

import BasicButton from "../button/BasicButton";
import FaIcon from "../icon/FaIcon";

const Root = styled("div")`
  display: flex;
`;

const Left = styled("span")<{ running?: boolean }>`
  transition: ${({ theme }) =>
    `color ${theme.transitionTime}, background-color ${theme.transitionTime}`};
  padding: 0 15px;
  background-color: ${({ theme, running }) =>
    running ? "white" : theme.col.blueGrayDark};
  border-right: ${({ theme, running }) =>
    running ? `1px solid ${theme.col.blueGrayDark}` : "transparent"};
`;

const Label = styled("span")`
  transition: background-color ${({ theme }) => theme.transitionTime};
  padding: 0 15px;
  color: ${({ theme }) => theme.col.black};
  background-color: white;
  line-height: 2.5;
  white-space: nowrap;
`;

const StyledBasicButton = styled(BasicButton)`
  outline: none;
  border: 1px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
  overflow: hidden;
  padding: 0;
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  line-height: 2.5;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  &:hover {
    ${Label} {
      background-color: ${({ theme }) => theme.col.grayVeryLight};
    }
  }
`;

function getIcon(loading: boolean, running: boolean) {
  return loading ? faSpinner : running ? faStop : faPlay;
}

interface Props {
  isStartStopLoading: boolean;
  isQueryRunning: boolean;
  disabled: boolean;
  onClick: () => void;
}

// A button that is prefixed by an icon
const QueryRunnerButton = forwardRef<HTMLDivElement, Props>(
  ({ onClick, isStartStopLoading, isQueryRunning, disabled }, ref) => {
    const { t } = useTranslation();
    const label = isQueryRunning
      ? t("queryRunner.stop")
      : t("queryRunner.start");

    const icon = getIcon(isStartStopLoading, isQueryRunning);

    return (
      <Root ref={ref}>
        <StyledBasicButton
          type="button"
          onClick={onClick}
          disabled={disabled}
          data-test-id="query-runner-button"
        >
          <Left running={isQueryRunning}>
            <FaIcon white={!isQueryRunning} icon={icon} />
          </Left>
          <Label>{label}</Label>
        </StyledBasicButton>
      </Root>
    );
  },
);

export default QueryRunnerButton;
