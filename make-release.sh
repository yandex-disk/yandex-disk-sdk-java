#!/bin/sh

(cd sdk && rm -rf build.gradle settings.gradle gradle gradlew gradlew.bat)
(cd sdk-example && rm -rf build.gradle settings.gradle gradle gradlew gradlew.bat my-release-key.keystore)
rm -rf README.md make-release.sh .git .gitignore

