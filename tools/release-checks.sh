#!/bin/sh

APPDIR=Simplenote

function pOk() {
  echo "[$(tput setaf 2)OK$(tput sgr0)]"
}

function pFail() {
  echo "[$(tput setaf 1)KO$(tput sgr0)]"
}

function printVersion() {
  gradle_version=$(grep -E 'version ".*"' $APPDIR/build.gradle | sed s/version// | grep -Eo "[a-zA-Z0-9.-]+" )
  echo "$APPDIR/build.gradle version $gradle_version"
}

function checkFileAgainstHash() {
  filename=$1
  known_checksum=$2
  checksum=`sha1sum "$filename" | cut -d" " -f1`
  if [ x$checksum != x$known_checksum ]; then
    pFail
    exit 6
  fi
  pOk
}

function checkGradleProperties() {
  /bin/echo -n "Check gradle.properties..."
  checkFileAgainstHash $APPDIR/gradle.properties 2e36d6696bc71af3bc976b838966ac40cf06e4fe
}

function checkKeystore() {
  keystore=`cat gradle.properties | grep storeFile | cut -d= -f 2 | sed -e 's/^[ \t]*//'`
  /bin/echo -n "Check Keystore..."
  checkFileAgainstHash "$APPDIR/$keystore" 7b20577a43b217b668fa875693c006d693679c0c
}

checkGradleProperties
checkKeystore
printVersion
