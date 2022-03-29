import styled from "@emotion/styled";
import type { StateT } from "app-types";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { SelectOptionT, UserGroupT } from "../../api/types";
import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import FaIcon from "../../icon/FaIcon";
import Modal from "../../modal/Modal";
import InputMultiSelect from "../../ui-components/InputMultiSelect/InputMultiSelect";

import type { ProjectItemT } from "./ProjectItem";
import {
  useLoadQuery,
  useLoadFormConfig,
  useUpdateQuery,
  useUpdateFormConfig,
} from "./actions";
import { isFormConfig } from "./helpers";

const Buttons = styled("div")`
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

const SxPrimaryButton = styled(PrimaryButton)`
  margin-left: 20px;
`;

const SxInputMultiSelect = styled(InputMultiSelect)`
  display: block;
  margin-bottom: 20px;
`;

const QueryName = styled("p")`
  margin: -15px 0 20px;
`;

const getInitialUserGroupsValue = (
  userGroups: UserGroupT[],
  projectItem?: ProjectItemT,
) => {
  return projectItem && projectItem.groups
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

const ShareProjectItemModal = ({ item, onClose }: PropsT) => {
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
    <Modal onClose={onClose} headline={t("sharePreviousQueryModal.headline")}>
      <QueryName>{item.label}</QueryName>
      <SxInputMultiSelect
        value={userGroupsValue}
        onChange={onSetUserGroupsValue}
        label={t("sharePreviousQueryModal.groupsLabel")}
        options={userGroupOptions}
      />
      <Buttons>
        <TransparentButton onClick={onClose}>
          {t("common.cancel")}
        </TransparentButton>
        <SxPrimaryButton onClick={onShareClicked} disabled={buttonDisabled}>
          <>
            {loading && (
              <>
                <FaIcon white icon="spinner" />{" "}
              </>
            )}
            {shareLabel}
          </>
        </SxPrimaryButton>
      </Buttons>
    </Modal>
  );
};

export default ShareProjectItemModal;
