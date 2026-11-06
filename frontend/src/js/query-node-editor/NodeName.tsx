import { memo, useState } from "react";
import { useTranslation } from "react-i18next";

import EditableText from "../ui-components/EditableText";
import { C2 } from "../ui-components/Typography";

interface Props {
  allowEditing: boolean;
  label: string;
  onUpdateLabel: (label: string) => void;
}

const NodeName = ({ allowEditing, label, onUpdateLabel }: Props) => {
  const { t } = useTranslation();
  const [editingLabel, setEditingLabel] = useState<boolean>(false);

  return (
    <div className="min-w-0">
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
        <C2 truncate>{label}</C2>
      )}
    </div>
  );
};

export default memo(NodeName);
