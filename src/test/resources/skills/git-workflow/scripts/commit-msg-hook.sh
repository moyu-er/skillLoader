#!/bin/bash
# Git commit-msg hook for conventional commits

COMMIT_MSG_FILE=$1
COMMIT_MSG=$(head -1 $COMMIT_MSG_FILE)
PATTERN="^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .+$"

if [[ $COMMIT_MSG =~ $PATTERN ]]; then
    exit 0
else
    echo "Error: Commit message does not follow conventional commit format"
    echo "Example: feat(auth): add login functionality"
    exit 1
fi
