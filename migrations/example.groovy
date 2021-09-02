/**
 * Example script for MigrateCommand, deleting all Executions that are for dataset `example_dataset`.
 */

import com.fasterxml.jackson.databind.node.ObjectNode


return {
    String env, String store, String key, ObjectNode value ->
        if (store != "EXECUTIONS") {
            return new Tuple(key, value)
        }

        if (value.get("dataset").asText() == "example_dataset") {
            return null
        }

        return new Tuple(key, value)
}