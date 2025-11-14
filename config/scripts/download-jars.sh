#!/bin/bash

download_jar() {
  jar_name="$1"
  version="$2"
  suffix="$3"
  repo="$4"
  target="/downloads/$jar_name.jar"

  if [[ -f "$target" ]]; then
    echo "$jar_name already exists at $target, skipping download"
    return
  fi

  if [[ -n "$version" && -n "$suffix" ]]; then
    echo "Downloading $jar_name (version: $version, suffix: $suffix)"
    curl -fSL -u "$NEXUS_USER:$NEXUS_PASS" \
      "$NEXUS_URL/repository/$repo/dev/markodojkic/$jar_name/$version/$jar_name-$suffix.jar" \
      -o "$target"
  else
    echo "Skipping $jar_name download - version or suffix not provided"
  fi
}

download_jar "SingiAttend-Server" "$BE_JAR_VERSION" "$BE_JAR_SUFFIX" "maven-snapshots"
download_jar "eurekaserver" "$EUREKA_JAR_VERSION" "$EUREKA_JAR_SUFFIX" "maven-releases"
download_jar "SingiAttend-Student_Proxy" "$STUDENT_PROXY_JAR_VERSION" "$STUDENT_PROXY_JAR_SUFFIX" "maven-releases"
