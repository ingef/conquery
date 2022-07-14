import styled from "@emotion/styled";
import { memo, useState } from "react";
import { useTranslation } from "react-i18next";

import EditableText from "../ui-components/EditableText";

const Root = styled("div")`
  padding: 10px 15px;
`;

interface Props {
  allowEditing: boolean;
  maxWidth?: number;
  label: string;
  onUpdateLabel: (label: string) => void;
}

const NodeName = ({ allowEditing, label, maxWidth, onUpdateLabel }: Props) => {
  const { t } = useTranslation();
  const [editingLabel, setEditingLabel] = useState<boolean>(false);

  return (
    <Root style={{ maxWidth }}>
      {allowEditing ? (
        <EditableText
          large
          loading={false}
          text={label}
          tooltip={t("help.editConceptName")}
          selectTextOnMount={true}
          editing={editingLabel}
          onSubmit={(value) => {
            onUpdateLabel(value);
            setEditingLabel(false);
          }}
          onToggleEdit={() => setEditingLabel(!editingLabel)}
        />
      ) : (
        label
      )}
    </Root>
  );
};

export default memo(NodeName);
