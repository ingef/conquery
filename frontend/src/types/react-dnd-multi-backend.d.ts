// @types/react-dnd-multi-backend isn't up to date and doesn't know about usePreview yet

declare module "react-dnd-multi-backend" {
  export const usePreview: any;
  export const TouchTransition: any;

  let x: any;
  export default x;
}
