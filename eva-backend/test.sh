#!/bin/bash

## For JSON you need the jq api: sudo apt-get install jq - https://stedolan.github.io/jq/

command -v jq >/dev/null 2>&1 || { echo >&2 "I require jq but it's not installed.\rsudo apt-get install jq - https://stedolan.github.io/jq/."; exit 1; }

function usage() {
    cat << EOF
    usage: $0 options
    Example: ./systemtest.sh -a /tmp -b path/to/csv -c path/to/conceptTrees -d path/to/config.json

    OPTIONS:
       -a  Path to Working directory
       -b  Path to *.csv and *.discription.json files
       -c  Path to conceptTrees
       -d  Path to config file 
EOF
}

function java_state () {
    if [ $1 -ne 0 ]; then
        echo "Failed to execute Java"
        exit 1
    fi
}

while getopts "a:b:c:d:h" opt; do
    case $opt in
        a)
            WORKING_DIR=$OPTARG
            ;;
        b)
            CSV=$OPTARG
            ;;
        c)
            CONCEPT_TREES=$OPTARG
            ;;
        d)
            CONFIG_FILE=$OPTARG
            ;;
        h)
            usage
            exit 1
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            usage
            exit 1
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            exit 1
            ;;
    esac
done

## check working dir is not empty
if [[ -z $WORKING_DIR ]]; then WORKING_DIR=/tmp; fi

## required parameters
if [[ -z $CSV ]] || [[ -z $CONCEPT_TREES ]] || [[ -z $CONFIG_FILE ]]
then
    usage
    exit 1
fi

## basic parameters
CURRENT_DIR=$PWD
CONFIG_FILE_NAME=$(basename $CONFIG_FILE)

## create unique working path
WORKING_DIR=$WORKING_DIR/$(uuidgen)-systemtest
echo "working directory: $WORKING_DIR"

mkdir -p $WORKING_DIR
mkdir -p $WORKING_DIR/conceptTrees
mkdir -p $WORKING_DIR/preproc
mkdir -p $WORKING_DIR/logs

cd $CSV
## Extract csv header and last 1000 lines	
for f in *.csv ; do
    cat $f | head -n 1000 > $WORKING_DIR/$f
done
## Copy all *.description.json to $WORKING_DIR
cp *.description.json $WORKING_DIR/
cp -R $CONCEPT_TREES/* $WORKING_DIR/conceptTrees/

jq 	--arg path1 $WORKING_DIR \
	--arg path2 $WORKING_DIR/preproc/ \
	--arg path3 $WORKING_DIR/conceptTrees/ \
' .importer.datasets[].conceptsDirectory=$path3 
| .importer.datasets[].preprocessedDirectory=$path2 
| .preprocessor.directories[].csv=$path1
| .preprocessor.directories[].descriptions=$path1
| .preprocessor.directories[].preprocessedOutput=$path2' \
$CONFIG_FILE > $WORKING_DIR/t.$CONFIG_FILE_NAME && mv $WORKING_DIR/t.$CONFIG_FILE_NAME $WORKING_DIR/$CONFIG_FILE_NAME

echo -ne '#####                      (20%)\r'

JAR_PATTERN="ingef-eva-*.jar"
JAR_FILE=$(find $CURRENT_DIR/target -name $JAR_PATTERN)
cp $JAR_FILE $WORKING_DIR/
JAR_FILE=$(find $WORKING_DIR -name $JAR_PATTERN)

echo -ne '##########                 (40%)\r'
java -jar $JAR_FILE preprocess $WORKING_DIR/$CONFIG_FILE_NAME > $WORKING_DIR/logs/preprocess.log
java_state $?

echo -ne '###############            (60%)\r'
java -jar $JAR_FILE import $WORKING_DIR/$CONFIG_FILE_NAME > $WORKING_DIR/logs/import.log
java_state $?

echo -ne '#################          (70%)\r'
## starting Webservice: See config.json eg http://localhost:8080/api/datasets/pseudo1m/concepts
java -jar $JAR_FILE server $WORKING_DIR/$CONFIG_FILE_NAME > $WORKING_DIR/logs/server.log &
SERVER_PID=$!
java_state $?

echo -ne '####################       (80%)\r'
## testing webservice is alive and basic functions
function generate_post_data() {
cat << EOF
	{"version":$1,"type":"CONCEPT_QUERY","groups":[{"elements":[{"id":"icd.c00_d48.c30_c39","type":"CONCEPT","tables":[{"id":"icd.kh_diagnose_icd_code","filters":[]},{"id":"icd.au_fall","filters":[]},{"id":"icd.arzt_diagnose_icd_code","filters":[]}]}]}]}
EOF
}
## get version
VERSION=($(jq -r '.version' <<< $(curl --silent http://localhost:8080/api/datasets/pseudo1m/concepts)))
## create post request with json data; See: generate_post_data function
QUERY_ID=$(jq -r '.id' <<< $(curl --silent \
-H "Accept: application/json" \
-H "Content-Type:application/json" \
-X POST --data "$(generate_post_data $VERSION)" "http://localhost:8080/api/datasets/pseudo1m/queries"))
## evaluate http code 200-299
TEST_RES=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:8080/api/datasets/pseudo1m/queries/$QUERY_ID)

test "$TEST_RES" -ge 200 && test "$TEST_RES" -le 299

## Clearing working dir
rm -rf $WORKING_DIR

## stopping webservice
kill $SERVER_PID

echo -ne '#########################  (100%) - Succesfully completed\r'
echo -ne '\n'
