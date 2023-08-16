#!/bin/bash

set -e

cd "$( cdlocal "$0" )"

CURRENT_DIR=$( pwd )
# Because maven.herb uses jenkins user instead of root
WORK_DIR="/home/jenkins/workspace"
M2_DIR="${HOME}/.m2"

docker run -it --rm \
	-v "${M2_DIR}":/home/jenkins/.m2 \
	-v "${CURRENT_DIR}":"${WORK_DIR}" \
	-w "${WORK_DIR}" \
	docker.herb.herbmarshall.com/maven.herb \
	mvn clean install
