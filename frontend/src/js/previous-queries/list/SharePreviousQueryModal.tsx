import styled from "@emotion/styled";
import { StateT } from "app-types";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { usePatchQuery } from "../../api/api";
import type { DatasetIdT, SelectOptionT, UserGroupT } from "../../api/types";
import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import { exists } from "../../common/helpers/exists";
import { usePrevious } from "../../common/helpers/usePrevious";
import Modal from "../../modal/Modal";
import { setMessage } from "../../snack-message/actions";
import InputMultiSelect from "../../ui-components/InputMultiSelect/InputMultiSelect";

import { useLoadQuery, shareQuerySuccess } from "./actions";
import { PreviousQueryT } from "./reducer";

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

interface PropsT {
  previousQueryId: string;
  onClose: () => void;
  onShareSuccess: () => void;
}

const getUserGroupsValue = (
  userGroups: UserGroupT[],
  previousQuery?: PreviousQueryT,
) => {
  return previousQuery && previousQuery.groups
    ? userGroups
        .filter((group) => previousQuery.groups?.includes(group.id))
        .map((group) => ({
          label: group.label,
          value: group.id,
        }))
    : [];
};

const SharePreviousQueryModal = ({
  previousQueryId,
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
  const previousQuery = useSelector<StateT, PreviousQueryT | undefined>(
    (state) =>
      state.previousQueries.queries.find(
        (query) => query.id === previousQueryId,
      ),
  );
  const initialUserGroupsValue = getUserGroupsValue(userGroups, previousQuery);

  const [userGroupsValue, setUserGroupsValue] = useState<SelectOptionT[]>(
    initialUserGroupsValue,
  );

  const previousPreviousQueryId = usePrevious(previousQueryId);

  const patchQuery = usePatchQuery();

  const dispatch = useDispatch();
  const loadQuery = useLoadQuery();

  useEffect(() => {
    if (
      exists(datasetId) &&
      !exists(previousPreviousQueryId) &&
      exists(previousQueryId)
    ) {
      loadQuery(datasetId, previousQueryId);
    }
  }, [datasetId, previousPreviousQueryId, previousQueryId]);

  useEffect(() => {
    setUserGroupsValue(getUserGroupsValue(userGroups, previousQuery));
  }, [userGroups, previousQuery]);

  const onSetUserGroupsValue = (value: SelectOptionT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  if (!previousQuery) {
    return null;
  }

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.id,
  }));

  async function onShareClicked() {
    if (!datasetId) return;

    const userGroupsToShare = userGroupsValue.map(
      (group) => group.value as string,
    );

    try {
      await patchQuery(datasetId, previousQueryId, {
        groups: userGroupsToShare,
      });

      dispatch(
        shareQuerySuccess({
          queryId: previousQueryId,
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
      <QueryName>{previousQuery.label}</QueryName>
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
        <SxPrimaryButton onClick={onShareClicked}>
          {previousQuery.shared && userGroupsValue.length === 0
            ? t("sharePreviousQueryModal.unshare")
            : t("common.share")}
        </SxPrimaryButton>
      </Buttons>
    </Modal>
  );
};

export default SharePreviousQueryModal;
