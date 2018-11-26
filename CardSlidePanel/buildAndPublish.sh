#!/bin/bash

function echoColorText() {
    echo -e "\033[3${1}m ==========${2}========== \033[0m"
}

function echoBlue() {
    echoColorText 6 $1
}

function echoGreen() {
    echoColorText 2 $1
}

function echoRed() {
    echoColorText 1 $1
}


echoBlue "开始编译"
./gradlew clean assembleRelease

code=$?
echo "status=$code"

[ $code -eq 0 ] && echoGreen "编译成功" || echoRed "编译失败"


if [ $code -eq 0 ]; then
	echoBlue "开始发布"
	./gradlew javadocJar
    ./gradlew sourcesJar
    ./gradlew install
    ./gradlew bintrayUpload
	code=$?
	[ $code -eq 0 ] && echoGreen "发布成功" || echoRed "发布失败"
fi
