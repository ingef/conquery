import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { SelectOptionT } from "../../api/types";
import { Button } from "../../ui-components/Button";
import { ComboBoxMultiField } from "../../ui-components/ComboBoxMultiField";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";
import { useUpdateFormConfig, useUpdateQuery } from "./actions";
import { isFormConfig } from "./helpers";
import type { ProjectItemT } from "./ProjectItem";
import { useFolders } from "./selector";

const toOption = (folder: string): SelectOptionT => ({
  label: folder,
  value: folder,
});

interface Props {
  item: ProjectItemT;
  onClose: () => void;
}

const EditProjectItemFoldersModalContent = ({ item, onClose }: Props) => {
  const { t } = useTranslation();
  const folders = useFolders();
  const [value, setValue] = useState(() => item.tags.map(toOption));
  const { loading: queryLoading, updateQuery } = useUpdateQuery();
  const { loading: formConfigLoading, updateFormConfig } =
    useUpdateFormConfig();

  const loading = queryLoading || formConfigLoading;

  const tags = value.map((option) => String(option.value));
  const unchanged =
    tags.length === item.tags.length &&
    tags.every((tag) => item.tags.includes(tag));

  const onSubmit = async () => {
    if (isFormConfig(item)) {
      await updateFormConfig(item.id, { tags }, t("formConfig.retagError"));
    } else {
      await updateQuery(item.id, { tags }, t("previousQuery.retagError"));
    }
    onClose();
  };

  return (
    <>
      <ModalHeader subtitle={item.label}>
        {t("editPreviousQueryFoldersModal.headline")}
      </ModalHeader>
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit();
        }}
      >
        <ModalBody>
          <ComboBoxMultiField
            label={t("inputMultiSelect.folders")}
            creatable
            autoFocus
            value={value}
            options={folders.map(toOption)}
            onChange={setValue}
            placeholder={t("inputMultiSelect.tagPlaceholder")}
          />
        </ModalBody>
        <ModalFooter>
          <Button slot="close">{t("common.cancel")}</Button>
          <Button
            intent="primary"
            type="submit"
            isDisabled={loading || unchanged}
          >
            {t("common.save")}
          </Button>
        </ModalFooter>
      </form>
    </>
  );
};

// opened by the list, not a DialogTrigger in the row: rows remount when the list reloads
const EditProjectItemFoldersModal = (props: Props) => (
  <Modal
    isOpen
    onOpenChange={(isOpen) => {
      if (!isOpen) props.onClose();
    }}
  >
    <EditProjectItemFoldersModalContent {...props} />
  </Modal>
);

export default EditProjectItemFoldersModal;
