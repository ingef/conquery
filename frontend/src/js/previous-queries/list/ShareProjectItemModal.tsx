import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import type { SelectOptionT, UserGroupT } from "../../api/types";
import type { StateT } from "../../app/reducers";
import { Button } from "../../ui-components/Button";
import { ComboBoxMultiField } from "../../ui-components/ComboBoxMultiField";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";
import {
  useLoadFormConfig,
  useLoadQuery,
  useUpdateFormConfig,
  useUpdateQuery,
} from "./actions";
import { isFormConfig } from "./helpers";
import type { ProjectItemT } from "./ProjectItem";

const getInitialUserGroupsValue = (
  userGroups: UserGroupT[],
  projectItem?: ProjectItemT,
) => {
  return projectItem?.groups
    ? userGroups
        .filter((group) => projectItem.groups?.includes(group.id))
        .map((group) => ({
          label: group.label,
          value: group.id,
        }))
    : [];
};

interface PropsT {
  item: ProjectItemT;
  onClose: () => void;
}

const ShareProjectItemModalContent = ({ item, onClose }: PropsT) => {
  const { t } = useTranslation();
  const userGroups = useSelector<StateT, UserGroupT[]>((state) =>
    state.user.me ? state.user.me.groups : [],
  );

  const initialUserGroupsValue = useMemo(
    () => getInitialUserGroupsValue(userGroups, item),
    [item, userGroups],
  );

  const [userGroupsValue, setUserGroupsValue] = useState<SelectOptionT[]>(
    initialUserGroupsValue,
  );

  const [loadedOnce, setLoadedOnce] = useState(false);

  const { loadQuery } = useLoadQuery();
  const { loadFormConfig } = useLoadFormConfig();
  const { updateQuery, loading: queryLoading } = useUpdateQuery();
  const { updateFormConfig, loading: formConfigLoading } =
    useUpdateFormConfig();

  const loading = queryLoading || formConfigLoading;

  useEffect(
    function loadItemOnce() {
      if (!loadedOnce) {
        setLoadedOnce(true);

        if (isFormConfig(item)) {
          loadFormConfig(item.id);
        } else {
          loadQuery(item.id);
        }
      }
    },
    [loadQuery, loadFormConfig, loadedOnce, item],
  );

  useEffect(() => {
    setUserGroupsValue(getInitialUserGroupsValue(userGroups, item));
  }, [userGroups, item]);

  const onSetUserGroupsValue = (value: SelectOptionT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.id,
  }));

  const shareLabel =
    item.shared && userGroupsValue.length === 0
      ? t("sharePreviousQueryModal.unshare")
      : t("common.share");
  const groupsLabel = isFormConfig(item)
    ? t("sharePreviousQueryModal.groupsLabelConfig")
    : t("sharePreviousQueryModal.groupsLabelQuery");

  const buttonDisabled =
    JSON.stringify(initialUserGroupsValue) === JSON.stringify(userGroupsValue);

  async function onShareClicked() {
    const userGroupsToShare = userGroupsValue.map(
      (group) => group.value as string,
    );

    if (isFormConfig(item)) {
      await updateFormConfig(
        item.id,
        {
          groups: userGroupsToShare,
        },
        t("formConfig.shareError"),
      );
    } else {
      await updateQuery(
        item.id,
        {
          groups: userGroupsToShare,
        },
        t("previousQuery.shareError"),
      );
    }
    onClose();
  }

  return (
    <>
      <ModalHeader subtitle={item.label}>
        {t("sharePreviousQueryModal.headline")}
      </ModalHeader>
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          onShareClicked();
        }}
      >
        <ModalBody>
          <ComboBoxMultiField
            autoFocus
            value={userGroupsValue}
            onChange={onSetUserGroupsValue}
            label={groupsLabel}
            options={userGroupOptions}
          />
        </ModalBody>
        <ModalFooter>
          <Button slot="close">{t("common.cancel")}</Button>
          <Button
            intent="primary"
            type="submit"
            isDisabled={loading || buttonDisabled}
          >
            {shareLabel}
          </Button>
        </ModalFooter>
      </form>
    </>
  );
};

// opened by the list, not a DialogTrigger in the row: rows remount when the list reloads
const ShareProjectItemModal = (props: PropsT) => (
  <Modal
    isOpen
    onOpenChange={(isOpen) => {
      if (!isOpen) props.onClose();
    }}
  >
    <ShareProjectItemModalContent {...props} />
  </Modal>
);

export default ShareProjectItemModal;
