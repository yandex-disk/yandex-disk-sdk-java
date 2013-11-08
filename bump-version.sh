if [ -z "$1" ]
then
  echo "No argument supplied"
else
  version=${1}
  buildfile=sdk/build.gradle
  sed -ir "s/version = '.*'/version = '${version}'/g" ${buildfile}
  rm ${buildfile}r
  git diff
fi
