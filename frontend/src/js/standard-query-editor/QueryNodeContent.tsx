import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import ErrorMessage from "../error-message/ErrorMessage";
import WithTooltip from "../tooltip/WithTooltip";

const Label = styled("p")`
  margin: 0;
  word-break: break-word;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.md};
`;
const Description = styled("p")`
  margin: 3px 0 0;
  word-break: break-word;
  line-height: 1.2;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
`;

const PreviousQueryLabel = styled("p")`
  margin: 0 0 3px;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const StyledErrorMessage = styled(ErrorMessage)`
  margin: 0;
`;

const RootNode = styled("p")`
  margin: 0 0 4px;
  line-height: 1;
  text-transform: uppercase;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  word-break: break-word;
`;

const Node = styled("div")`
  flex-grow: 1;
  padding-top: 2px;
`;

interface Props {
  tooltipText?: string;
  isConceptQueryNode?: boolean;
  error?: string;
  label: string;
  description?: string;
  rootNodeLabel: string | null;
}

const QueryNodeContent = ({
  tooltipText,
  error,
  label,
  description,
  rootNodeLabel,
  isConceptQueryNode,
}: Props) => {
  const { t } = useTranslation();

  return (
    <WithTooltip text={tooltipText}>
      <Node>
        {!isConceptQueryNode && (
          <PreviousQueryLabel>
            {t("queryEditor.previousQuery")}
          </PreviousQueryLabel>
        )}
        {error ? (
          <StyledErrorMessage message={error} />
        ) : (
          <>
            {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
            <Label>{label}</Label>
            {description && <Description>{description}</Description>}
          </>
        )}
      </Node>
    </WithTooltip>
  );
};

export default memo(QueryNodeContent);
