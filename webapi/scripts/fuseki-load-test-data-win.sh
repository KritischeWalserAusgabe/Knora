#!/usr/bin/env bash

curl -X POST -H "Content-type:application/x-www-form-urlencoded" --data-urlencode update='DROP ALL' http://localhost:3030/knora-test/update > /dev/null
curl -F filedata=@..\\..\\knora-ontologies\\knora-base.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/knora-base > /dev/null
curl -F filedata=@..\\..\\knora-ontologies\\knora-admin.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/knora-base > /dev/null
curl -F filedata=@..\\..\\knora-ontologies\\standoff-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/standoff > /dev/null
curl -F filedata=@..\\..\\knora-ontologies\\standoff-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/standoff > /dev/null
curl -F filedata=@..\\..\\knora-ontologies\\salsah-gui.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/salsah-gui > /dev/null
curl -F filedata=@..\\_test_data\\all_data\\admin-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/admin > /dev/null
curl -F filedata=@..\\_test_data\\all_data\\permissions-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/permissions > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\incunabula-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/incunabula > /dev/null
# curl -F filedata=@..\\_test_data\\all_data\\incunabula-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/incunabula > /dev/null
curl -F filedata=@..\\_test_data\\ontologies\\images-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/00FF/images > /dev/null
curl -F filedata=@..\\_test_data\\demo_data\\images-demo-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/00FF/images > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\anything-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/anything > /dev/null
# curl -F filedata=@..\\_test_data\\all_data\\anything-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/anything > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\beol-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/0801/beol > /dev/null
# curl -F filedata=@..\\_test_data\\all_data\\beol-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/beol > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\biblio-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/biblio > /dev/null
# curl -F filedata=@..\\_test_data\\all_data\\biblio-data.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/biblio > /dev/null

# curl -F filedata=@..\\_test_data\\ontologies\\text-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/text > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\human-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/human > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\image-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/image > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\work-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/work > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\scholasticism-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/scholasticism > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\event-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/event > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\publication-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/publication > /dev/null
# curl -F filedata=@..\\_test_data\\ontologies\\agent-ontology-knora.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/agent > /dev/null

curl -F filedata=@..\\_test_data\\ontologies\\walser-project.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/data/kwa
curl -F filedata=@..\\_test_data\\ontologies\\walser-onto.ttl http://localhost:3030/knora-test/data?graph=http://www.knora.org/ontology/kwa