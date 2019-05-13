
set -e

HOST="http://localhost"
PORT="7200"
GRAPHDB="${HOST}:${PORT}"
CONSOLE="openrdf-console/bin/win-console.sh --force --echo --serverURL $GRAPHDB"

GREEN='\033[0;32m'
RED='\033[0;31m'
NO_COLOUR='\033[0m'
DELIMITER="****************************************************************************************************\n* "

printf "\n\n${GREEN}${DELIMITER}Creating Lucene index${NO_COLOUR}\n\n"

STATUS=$(curl -s -w '%{http_code}' -S -X POST --data-urlencode 'update@./graphdb-knora-index-create.rq' $GRAPHDB/repositories/knora-test/statements)

if [ "$STATUS" == "204" ]
then
    printf "${GREEN}Lucene index built.${NO_COLOUR}\n\n"
else
    printf "${RED}Building of Lucene index failed: ${STATUS}${NO_COLOUR}\n\n"
fi