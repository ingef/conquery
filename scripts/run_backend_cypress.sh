#!/bin/env bash
script_dir=`dirname $0`

config=$(cat << EOF
 {
    "server": {
        "applicationConnectors": [
            {
                "type": "http",
                "port": "8080"
            }
        ],
		"adminConnectors": [
			{
				"type": "http",
				"port": "8081"
			}
		]
    }
}
EOF
)

java -jar "$(readlink -f $script_dir/../executable/target/executable*.jar)" standalone 