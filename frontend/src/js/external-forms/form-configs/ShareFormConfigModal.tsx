import React, { useEffect, useState } from "react";
import T from "i18n-react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";

import Modal from "../../modal/Modal";
import { patchFormConfig } from "../../api/api";
import type { DatasetIdT, UserGroupT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";
import TransparentButton from "../../button/TransparentButton";
import PrimaryButton from "../../button/PrimaryButton";
import { FormConfigT } from "./reducer";
import InputMultiSelect from "../../form-components/InputMultiSelect";
import { patchFormConfigSuccess } from "./actions";
import { usePrevious } from "../../common/helpers/usePrevious";
import { exists } from "../../common/helpers/exists";
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

const ShareFormConfigModal = ({
  formConfigId,
  onClose,
  onShareSuccess,
}: PropsT) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const userGroups = useSelector<StateT, UserGroupT[]>((state) =>
    state.user.me ? state.user.me.groups : []
  );
  const formConfig = useSelector<StateT, FormConfigT | undefined>((state) =>
    state.formConfigs.data.find((config) => config.id === formConfigId)
  );
  const initialUserGroupsValue =
    formConfig && formConfig.groups
      ? userGroups
          .filter((group) => formConfig.groups?.includes(group.groupId))
          .map((group) => ({ label: group.label, value: group.groupId }))
      : [];

  const [userGroupsValue, setUserGroupsValue] = useState<SelectValueT[]>(
    initialUserGroupsValue
  );

  const previousFormConfigId = usePrevious(formConfigId);

  const { loadFormConfig } = useLoadFormConfig();

  useEffect(() => {
    if (
      exists(datasetId) &&
      !exists(previousFormConfigId) &&
      exists(formConfigId)
    ) {
      loadFormConfig(datasetId, formConfigId);
    }
  }, [datasetId, previousFormConfigId, formConfigId, loadFormConfig]);

  const dispatch = useDispatch();

  const onSetUserGroupsValue = (value: SelectValueT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  if (!formConfig) {
    return null;
  }

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.groupId,
  }));

  async function onShareClicked() {
    if (!datasetId) return;

    const shared = userGroupsValue.length > 0;
    const userGroupsToShare = userGroupsValue.map((group) => group.value);

    try {
      await patchFormConfig(datasetId, formConfigId, {
        shared,
        groups: userGroupsToShare,
      });

      dispatch(
        patchFormConfigSuccess(formConfigId, {
          ...formConfig,
          shared,
          groups: userGroupsToShare,
        })
      );

      onShareSuccess();
    } catch (e) {
      dispatch(setMessage("previousQuery.shareError"));
    }
  }

  return (
    <Modal
      onClose={onClose}
      headline={T.translate("sharePreviousQueryModal.headline")}
    >
      <QueryName>{formConfig.label}</QueryName>
      <SxInputMultiSelect
        input={{ value: userGroupsValue, onChange: onSetUserGroupsValue }}
        label={T.translate("sharePreviousQueryModal.groupsLabel")}
        options={userGroupOptions}
      />
      <Buttons>
        <Btn onClick={onClose}>{T.translate("common.cancel")}</Btn>
        <PrimaryBtn onClick={onShareClicked}>
          {formConfig.shared && userGroupsValue.length === 0
            ? T.translate("sharePreviousQueryModal.unshare")
            : T.translate("common.share")}
        </PrimaryBtn>
      </Buttons>
    </Modal>
  );
};

export default ShareFormConfigModal;
