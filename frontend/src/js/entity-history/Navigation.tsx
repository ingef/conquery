import {
  faArrowDown,
  faArrowUp,
  faChevronLeft,
  faDownload,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import {
  type Dispatch,
  memo,
  type SetStateAction,
  useCallback,
  useMemo,
} from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { Button } from "../ui-components/Button";
import { ConfirmMenu } from "../ui-components/ConfirmMenu";
import { Icon } from "../ui-components/Icon";
import {
  Tooltip,
  TooltipTrigger,
  tooltipDelay,
} from "../ui-components/Tooltip";
import { closeHistory, resetHistory, useUpdateHistorySession } from "./actions";
import { EntityIdsList } from "./EntityIdsList";
import type { EntityIdsStatus } from "./History";
import {
  LoadHistoryDropzone,
  type LoadingPayload,
} from "./LoadHistoryDropzone";
import { NavigationHeader } from "./NavigationHeader";
import type { EntityId } from "./reducer";
import { SearchEntites } from "./SearchEntities";
import { saveHistory } from "./saveAndLoad";

const root = tv({
  base: ["grid", "gap-[10px]", "overflow-hidden", "bg-bg-50"],
});

const row = tv({
  base: ["flex", "gap-[10px]", "mr-[10px] ml-5"],
});

const entityIdNav = tv({
  base: ["flex flex-col", "overflow-hidden", "pr-[10px] pl-5"],
});

const loadHistoryDropzone = tv({
  base: [
    "block",
    "h-full",
    "grow",
    "overflow-y-auto",
    "p-[2px]",
    "text-inherit",
  ],
});

const containedButton = tv({
  base: ["grow", "justify-center"],
});

const fullWidthButton = tv({
  base: ["w-full", "justify-center"],
});

export const Navigation = memo(
  ({
    blurred,
    className,
    entityIds,
    entityIdsStatus,
    currentEntityId,
    currentEntityIndex,
    entityStatusOptions,
    setEntityStatusOptions,
    onLoadFromFile,
    onResetHistory,
  }: {
    blurred?: boolean;
    className?: string;
    entityIds: EntityId[];
    entityIdsStatus: EntityIdsStatus;
    currentEntityId: EntityId | null;
    currentEntityIndex: number;
    entityStatusOptions: SelectOptionT[];
    setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
    onLoadFromFile: (payload: LoadingPayload) => void;
    onResetHistory: () => void;
  }) => {
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const { loadingId, updateHistorySession } = useUpdateHistorySession();
    const onCloseHistory = useCallback(() => {
      dispatch(closeHistory());
    }, [dispatch]);

    const ids = useSelector<StateT, unknown[]>(
      (state) => state.entityHistory.entityIds,
    );

    const goToPrev = useCallback(() => {
      const prevIdx = Math.max(0, currentEntityIndex - 1);

      updateHistorySession({ entityId: entityIds[prevIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);
    const goToNext = useCallback(() => {
      const nextIdx = Math.min(entityIds.length - 1, currentEntityIndex + 1);

      updateHistorySession({ entityId: entityIds[nextIdx] });
    }, [entityIds, currentEntityIndex, updateHistorySession]);

    const onDownload = useCallback(() => {
      saveHistory({ entityIds, entityIdsStatus });
    }, [entityIds, entityIdsStatus]);

    const onReset = useCallback(() => {
      onResetHistory();
      dispatch(resetHistory({ includingDefaultParams: false }));
    }, [dispatch, onResetHistory]);

    useHotkeys("shift+up", goToPrev, [goToPrev]);
    useHotkeys("shift+down", goToNext, [goToNext]);

    const markedCount = useMemo(
      () => Object.values(entityIdsStatus).filter((v) => v.length > 0).length,
      [entityIdsStatus],
    );

    const backButtonWarning =
      markedCount > 0 ? t("history.backButtonWarning") : "";

    const empty = ids.length === 0;

    return (
      <div
        className={root({ className })}
        style={{
          gridTemplateRows: empty ? "auto 1fr" : "auto auto 1fr",
        }}
      >
        <div className={row()}>
          <TooltipTrigger>
            <Button
              intent="secondary"
              onPress={onCloseHistory}
              className={containedButton()}
            >
              <Icon icon={faChevronLeft} />
              {t("common.back")}
            </Button>
            <Tooltip>{backButtonWarning}</Tooltip>
          </TooltipTrigger>
          {!empty && (
            <ConfirmMenu
              onConfirm={onReset}
              confirmationText={t("history.settings.resetConfirm")}
            >
              <Button intent="secondary" className={containedButton()}>
                <Icon icon={faTrash} />
                {t("history.settings.reset")}
              </Button>
            </ConfirmMenu>
          )}
        </div>
        {!empty && (
          <NavigationHeader
            className="mr-[10px] ml-5"
            markedCount={markedCount}
            idsCount={entityIds.length}
            entityStatusOptions={entityStatusOptions}
            setEntityStatusOptions={setEntityStatusOptions}
          />
        )}
        <div className={entityIdNav()}>
          {!empty && (
            <div className="flex">
              <TooltipTrigger delay={tooltipDelay.long}>
                <Button
                  aria-label={`${t("history.prevButtonLabel")} (shift + ⬆)`}
                  intent="tertiary"
                  onPress={goToPrev}
                  className={fullWidthButton()}
                >
                  <Icon icon={faArrowUp} />
                </Button>
                <Tooltip>{`${t("history.prevButtonLabel")} (shift + ⬆)`}</Tooltip>
              </TooltipTrigger>
            </div>
          )}
          <LoadHistoryDropzone
            className={loadHistoryDropzone()}
            onLoadFromFile={onLoadFromFile}
          >
            {entityIds.length === 0 && (
              <SearchEntites onLoad={onLoadFromFile} />
            )}
            <EntityIdsList
              blurred={blurred}
              currentEntityId={currentEntityId}
              entityIds={entityIds}
              updateHistorySession={updateHistorySession}
              entityIdsStatus={entityIdsStatus}
              loadingId={loadingId}
            />
          </LoadHistoryDropzone>
          {!empty && (
            <>
              <div className="flex">
                <TooltipTrigger delay={tooltipDelay.long}>
                  <Button
                    aria-label={`${t("history.nextButtonLabel")} (shift + ⬇)`}
                    intent="tertiary"
                    onPress={goToNext}
                    className={fullWidthButton()}
                  >
                    <Icon icon={faArrowDown} />
                  </Button>
                  <Tooltip>{`${t("history.nextButtonLabel")} (shift + ⬇)`}</Tooltip>
                </TooltipTrigger>
              </div>
              <div className="flex" style={{ marginTop: "10px" }}>
                <TooltipTrigger>
                  <Button
                    intent="secondary"
                    onPress={onDownload}
                    className={[fullWidthButton(), "bg-white"]}
                  >
                    <Icon icon={faDownload} />
                    CSV
                  </Button>
                  <Tooltip>{t("history.downloadButtonLabel")}</Tooltip>
                </TooltipTrigger>
              </div>
            </>
          )}
        </div>
      </div>
    );
  },
);
