#!/bin/bash

# define action usage commands
usage() { echo "Usage: $0 [-v \"version\"]" >&2; exit 1; }

# set option arguments to variables and echo usage on failures
version=
while getopts ":v:" o; do
  case "${o}" in
    v)
      version="${OPTARG}"
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      usage
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "$version" ]; then
  echo "Version not specified" >&2
  exit 2
fi

changeVersionCommand="mvn versions:set -DnewVersion=${version}"
changeParentVersionCommand="mvn versions:update-parent -DallowSnapshots=true -DparentVersion=${version}"
mvnInstallCommand="mvn clean install"


(cd features && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"features\" folder" >&2; exit 3; fi
(cd features && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"features\" folder" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/api/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/api/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/core/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/core/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/persistence/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/persistence/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugin-kubernetes/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugin-kubernetes/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugin-mesos/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugin-mesos/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugin-mesos/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugin-mesos/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugin-nova/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugin-nova/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugins/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugins/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/plugins-api/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/plugins-api/feature.xml\"" >&2; exit 3; fi
sed -i 's/\(<version>\).*\(<\/version>\)/\1'${version}'\2/g' features/webservice/feature.xml
if [ $? -ne 0 ]; then echo "Failed to change version in \"features/webservice/feature.xml\"" >&2; exit 3; fi
(cd packages && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages\" folder" >&2; exit 3; fi
(cd packages && $changeParentVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven parent versions in the \"packages\" folder" >&2; exit 3; fi
(cd packages/attestation-hub-docker && $changeVersionCommand)
if [ $? -ne 0 ]; then echo "Failed to change maven version on \"packages/attestation-hub-docker\" folder" >&2; exit 3; fi
sed -i 's/\(attestation-hub-\).*\(.bin\)/\1'${version}'\2/g' packages/attestation-hub/pom.xml
if [ $? -ne 0 ]; then echo "Failed to change rpm binary script version in \"packages/attestation-hub/pom.xml\"" >&2; exit 3; fi
sed -i 's/\-[0-9\.]*\(\-SNAPSHOT\|\(\-\|\.zip$\|\.bin$\|\.jar$\)\)/-'${version}'\2/g' build.targets
if [ $? -ne 0 ]; then echo "Failed to change versions in \"build.targets\" file" >&2; exit 3; fi
