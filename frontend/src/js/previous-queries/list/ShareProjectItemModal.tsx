import styled from "@emotion/styled";
import { faCheck, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { SelectOptionT, UserGroupT } from "../../api/types";
import type { StateT } from "../../app/reducers";
import IconButton from "../../button/IconButton";
import Modal from "../../modal/Modal";
import WithTooltip from "../../tooltip/WithTooltip";
import InputMultiSelect from "../../ui-components/InputMultiSelect/InputMultiSelect";

import type { ProjectItemT } from "./ProjectItem";
import {
  useLoadQuery,
  useLoadFormConfig,
  useUpdateQuery,
  useUpdateFormConfig,
} from "./actions";
import { isFormConfig } from "./helpers";

const Row = styled("div")`
  width: 100%;
  display: flex;
  align-items: flex-end;
`;

const SxIconButton = styled(IconButton)`
  padding: 7px 10px;
  margin-left: 3px;
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
    <Modal
      onClose={onClose}
      headline={t("sharePreviousQueryModal.headline")}
      subtitle={item.label}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onShareClicked();
        }}
      >
        <Row>
          <InputMultiSelect
            autoFocus
            value={userGroupsValue}
            onChange={onSetUserGroupsValue}
            label={groupsLabel}
            options={userGroupOptions}
          />
          <WithTooltip text={shareLabel}>
            <SxIconButton
              type="submit"
              frame
              disabled={buttonDisabled}
              icon={loading ? faSpinner : faCheck}
            />
          </WithTooltip>
        </Row>
      </form>
    </Modal>
  );
};

export default ShareProjectItemModal;
