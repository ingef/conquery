import { QueryEditor } from "./QueryEditor";
import StandardQueryRunner from "./StandardQueryRunner";

// the editor takes the height left over by the runner row
const StandardQueryEditorTab = () => (
  <div className="grid grid-rows-[minmax(0,1fr)_auto]">
    <QueryEditor />
    <StandardQueryRunner />
  </div>
);

export default StandardQueryEditorTab;
