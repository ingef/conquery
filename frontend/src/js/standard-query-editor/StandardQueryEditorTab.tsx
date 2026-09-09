import { tv } from "tailwind-variants";

import { QueryEditor } from "./QueryEditor";
import StandardQueryRunner from "./StandardQueryRunner";

// the editor takes the height left over by the runner row
const root = tv({ base: ["grid grid-rows-[minmax(0,1fr)_auto]"] });

const StandardQueryEditorTab = () => (
  <div className={root()}>
    <QueryEditor />
    <StandardQueryRunner />
  </div>
);

export default StandardQueryEditorTab;
