import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import InputCheckbox from "../ui-components/InputCheckbox";

const Container = styled("div")`
  margin: 15px 10px;
`;
const Row = styled("div")`
  max-width: 300px;
  margin-bottom: 10px;
`;

interface Props {
  excludeTimestamps?: boolean;
  onToggleTimestamps?: (excludeTimestamps: boolean) => void;
  excludeFromSecondaryId?: boolean;
  onToggleSecondaryIdExclude?: (excludeFromSecondaryId: boolean) => void;
}

const CommonNodeSettings = ({
  excludeTimestamps,
  onToggleTimestamps,
  excludeFromSecondaryId,
  onToggleSecondaryIdExclude,
}: Props) => {
  const { t } = useTranslation();

  return (
    <Container>
      {onToggleTimestamps && (
        <Row>
          <InputCheckbox
            label={t("queryNodeEditor.excludeTimestamps")}
            tooltip={t("help.excludeTimestamps")}
            tooltipLazy
            value={excludeTimestamps}
            onChange={onToggleTimestamps}
          />
        </Row>
      )}
      {onToggleSecondaryIdExclude && (
        <Row>
          <InputCheckbox
            label={t("queryNodeEditor.excludeFromSecondaryId")}
            tooltip={t("help.excludeFromSecondaryId")}
            tooltipLazy
            value={excludeFromSecondaryId}
            onChange={onToggleSecondaryIdExclude}
          />
        </Row>
      )}
    </Container>
  );
};

export default memo(CommonNodeSettings);
