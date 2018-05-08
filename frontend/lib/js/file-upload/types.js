// @flow

export type DraggedFileType = {
    files: File[],
    isPreviousQuery?: void,
}

export type GenericFileType = {
    parameters: Object,
    callback?: Function,
    actionType?: string
}
