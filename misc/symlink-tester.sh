#!/usr/bin/env bash

usage() {
    echo usage: $0 path/to/sonarlint
    exit 1
}

test -f "$1" && test -x "$1" || usage

sonarlint=$1

if type mktemp &>/dev/null; then
    tempdir=$(mktemp -d)
else
    tempdir=/tmp/"$(basename "$0")-$$"
    mkdir -p "$tempdir"
fi

cleanup() {
    rm -fr "$tempdir"
}

trap 'cleanup; exit 1' 1 2 3 15

abspath() {
    (cd "$(dirname "$1")"; echo $PWD/"$(basename "$1")")
}

verify() {
    printf '%s -> ' "$1"
    shift
    "$@" &>/dev/null && echo ok || echo failed
}

relpath_to_root() {
    (
    cd "$1"
    relpath=.
    while test "$PWD" != /; do
        cd ..
        relpath=$relpath/..
    done
    echo $relpath
    )
}

ln -s "$(abspath "$sonarlint")" "$tempdir"/sonarlint
verify 'launch from abs symlink to abs path' "$tempdir"/sonarlint -h

ln -s $(relpath_to_root "$tempdir")/"$(abspath "$sonarlint")" "$tempdir"/sonarlint-rel
verify 'symlink to rel path is valid' test -f "$tempdir"/sonarlint-rel
verify 'launch from abs symlink to rel path' "$tempdir"/sonarlint-rel -h

mkdir "$tempdir/x"
ln -s ../sonarlint "$tempdir"/x/sonarlint
verify 'symlink to rel symlink is valid' test -f "$tempdir"/x/sonarlint
verify 'launch from abs symlink that is rel symlink to abs path' "$tempdir"/x/sonarlint -h

cleanup
