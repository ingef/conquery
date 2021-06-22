import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { usePatchFormConfig } from "../../api/api";
import type { DatasetIdT, UserGroupT } from "../../api/types";
import PrimaryButton from "../../button/PrimaryButton";
import TransparentButton from "../../button/TransparentButton";
import { exists } from "../../common/helpers/exists";
import { usePrevious } from "../../common/helpers/usePrevious";
import InputMultiSelect from "../../form-components/InputMultiSelect";
import Modal from "../../modal/Modal";
import { setMessage } from "../../snack-message/actions";

import { patchFormConfigSuccess } from "./actions";
import { FormConfigT } from "./reducer";
import { useLoadFormConfig } from "./selectors";

const Buttons = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

const SxInputMultiSelect = styled(InputMultiSelect)`
  display: block;
  margin-bottom: 20px;
`;

const QueryName = styled("p")`
  margin: -15px 0 20px;
`;

interface SelectValueT {
  label: string;
  value: string;
}

interface PropsT {
  formConfigId: string;
  onClose: () => void;
  onShareSuccess: () => void;
}

const getUserGroupsValue = (
  userGroups: UserGroupT[],
  formConfig?: FormConfigT,
) => {
  return formConfig && formConfig.groups
    ? userGroups
        .filter((group) => formConfig.groups?.includes(group.id))
        .map((group) => ({
          label: group.label,
          value: group.id,
        }))
    : [];
};

const ShareFormConfigModal = ({
  formConfigId,
  onClose,
  onShareSuccess,
}: PropsT) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const userGroups = useSelector<StateT, UserGroupT[]>((state) =>
    state.user.me ? state.user.me.groups : [],
  );
  const formConfig = useSelector<StateT, FormConfigT | undefined>((state) =>
    state.formConfigs.data.find((config) => config.id === formConfigId),
  );
  const initialUserGroupsValue = getUserGroupsValue(userGroups, formConfig);

  const [userGroupsValue, setUserGroupsValue] = useState<SelectValueT[]>(
    initialUserGroupsValue,
  );

  const previousFormConfigId = usePrevious(formConfigId);

  const { loadFormConfig } = useLoadFormConfig();
  const patchFormConfig = usePatchFormConfig();

  useEffect(() => {
    if (
      exists(datasetId) &&
      !exists(previousFormConfigId) &&
      exists(formConfigId)
    ) {
      loadFormConfig(datasetId, formConfigId);
    }
  }, [datasetId, previousFormConfigId, formConfigId, loadFormConfig]);

  useEffect(() => {
    setUserGroupsValue(getUserGroupsValue(userGroups, formConfig));
  }, [userGroups, formConfig]);

  const dispatch = useDispatch();

  const onSetUserGroupsValue = (value: SelectValueT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  if (!formConfig) {
    return null;
  }

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.id,
  }));

  async function onShareClicked() {
    if (!datasetId) return;

    const shared = userGroupsValue.length > 0;
    const userGroupsToShare = userGroupsValue.map((group) => group.value);

    try {
      await patchFormConfig(datasetId, formConfigId, {
        groups: userGroupsToShare,
      });

      dispatch(
        patchFormConfigSuccess(formConfigId, {
          ...formConfig,
          shared,
          groups: userGroupsToShare,
        }),
      );

      onShareSuccess();
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.shareError") }));
    }
  }

  return (
    <Modal onClose={onClose} headline={t("sharePreviousQueryModal.headline")}>
      <QueryName>{formConfig.label}</QueryName>
      <SxInputMultiSelect
        input={{ value: userGroupsValue, onChange: onSetUserGroupsValue }}
        label={t("sharePreviousQueryModal.groupsLabel")}
        options={userGroupOptions}
        closeMenuOnSelect
      />
      <Buttons>
        <Btn onClick={onClose}>{t("common.cancel")}</Btn>
        <PrimaryBtn onClick={onShareClicked}>
          {formConfig.shared && userGroupsValue.length === 0
            ? t("sharePreviousQueryModal.unshare")
            : t("common.share")}
        </PrimaryBtn>
      </Buttons>
    </Modal>
  );
};

export default ShareFormConfigModal;
