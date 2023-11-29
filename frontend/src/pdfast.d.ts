declare module "pdfast" {
    export interface createPdfastOptions {
        min: number,
        max: number,
        size: number,
        width: number,
    }
    export function create(arr: number[], options: createPdfastOptions): {x: number, y:number}[]
}