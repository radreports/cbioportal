#!/bin/bash

# exit when any of these fails
set -e
export DOCKER_IMAGE_CBIOPORTAL=cbioportal/cbioportal:demo-rfc72-squash

run_in_service() {
    service=$1
    shift
    docker-compose -f docker-compose.yml -f $PORTAL_SOURCE_DIR/test/integration/docker-compose-localbuild.yml \
        run --rm \
        "$service" bash -c "$@"
}

# load panels
echo "Testing the loading of gene panels..."
run_in_service cbioportal "cd /core/scripts/ && perl importGenePanel.pl --data \
                          /cbioportal/test/test_data/study_es_0/data_gene_panel_testpanel1.txt"
run_in_service cbioportal "cd /core/scripts/ && perl importGenePanel.pl --data \
                          /cbioportal/test/test_data/study_es_0/data_gene_panel_testpanel2.txt"
run_in_service cbioportal "cd /core/scripts/ && echo yes | ./importGenesetData.pl --data /cbioportal/test/test_data/genesets/study_es_0_genesets.gmt --new-version msigdb_7.5.1 --supp /cbioportal/test/test_data/genesets/study_es_0_supp-genesets.txt"
run_in_service cbioportal "cd /core/scripts/ && ./importGenesetHierarchy.pl --data /cbioportal/test/test_data/genesets/study_es_0_tree.yaml"

# dump portal info
echo "Testing the dump of local portal info directory..."
run_in_service cbioportal 'cd core/scripts/ && perl dumpPortalInfo.pl /cbioportal/portalinfo'

# validate study_es_0 using local portal info directory
echo "Testing validation based on local portalinfo..."
run_in_service cbioportal 'validateData.py -v -p /cbioportal/portalinfo -s /cbioportal/test/test_data/study_es_0/'

# load study_es_0 using API validation
echo "Testing loading of study with API validation..."
run_in_service cbioportal 'metaImport.py -v -u http://cbioportal-container:8080 -o -s /cbioportal/test/test_data/study_es_0/'

exit 0
