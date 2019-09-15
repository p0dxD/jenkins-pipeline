#!/usr/bin/bash
export GOPATH=$WORKSPACE
mkdir -p $GOPATH/src 
ln -f -s $WORKSPACE $GOPATH/src/main
go build