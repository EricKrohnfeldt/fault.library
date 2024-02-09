#!/bin/bash

set -e

#Using non-cdlocal because may be ran by jenkins
cd "$( dirname "$( realpath "$( readlink -f "$0" )" )" )"

OPERATION='build'
DEST=
BUILD_DIR=$( pwd )

while test $# -gt 0
do
		case "$1" in
			--docs)
				OPERATION='docs'
				;;
			*)
				DEST="$1"
				;;
		esac
		shift
done



if [[ "build" == "${OPERATION}" ]]; then

  # Because maven.herb uses jenkins user instead of root
  WORK_DIR="/home/jenkins/workspace"
  M2_DIR="${HOME}/.m2"

  docker run -it --rm \
    -v "${M2_DIR}":/home/jenkins/.m2 \
    -v "${BUILD_DIR}":"${WORK_DIR}" \
    -w "${WORK_DIR}" \
    docker.herb.herbmarshall.com/maven.herb \
    mvn clean install javadoc:javadoc

elif [[ "docs" == "${OPERATION}" ]]; then

  if [ -z "${DEST}" ]; then
    echo Destination folder required >&2
    exit 1
  elif [[ "${DEST}" != /* ]]; then
    DEST="${BUILD_DIR}/${DEST}"
  fi

  echo "Base: ${DEST}"

  for DOC_DIR in $( find . | grep -E 'target/site/apidocs$' | xargs realpath ); do
    cd "${DOC_DIR}/../.."
    ARTIFACT=$( find . -name '*.jar' | cut -c 3- | cut --delimiter=- -f1 | tail -n1 )
    VERSION=$( find . -name '*.jar' | cut -c 3- | cut --delimiter=- -f2 | tail -n1 )
    TARGET="${DEST}/${ARTIFACT}/${VERSION}"
    echo
    echo Processing: "${ARTIFACT} [ ${VERSION} ]   ->   ${TARGET}"
    mkdir -p "${DEST}/${ARTIFACT}/${VERSION}"
    cp -r "${DOC_DIR}"/* "${TARGET}"
    echo 'Complete'
    cd "${WORK_DIR}"
  done

fi
