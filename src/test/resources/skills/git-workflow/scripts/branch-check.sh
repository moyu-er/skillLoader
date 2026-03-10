#!/bin/bash
# Branch name validation script

BRANCH_NAME=$1
PATTERN="^(feature|bugfix|hotfix|release)\/[A-Z]+-[0-9]+-[a-z0-9-]+$"

if [[ $BRANCH_NAME =~ $PATTERN ]]; then
    echo "✓ Valid branch name"
    exit 0
else
    echo "✗ Invalid branch name. Expected format: type/PROJECT-123-description"
    exit 1
fi
