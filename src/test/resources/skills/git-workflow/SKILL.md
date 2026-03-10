---
name: git-workflow
description: Complete Git workflow guide with branching strategy, PR templates, and commit conventions
tags: [git, workflow, collaboration, best-practices]
author: SkillLoader Team
version: 2.0.0
license: MIT
---

# Git Workflow Guide

A comprehensive guide for professional Git workflows.

## Branch Strategy

### Main Branches
- `main` - Production ready code
- `develop` - Integration branch for features
- `hotfix/*` - Emergency fixes

### Feature Branches
```bash
git checkout -b feature/TICKET-123-short-description develop
```

## Commit Convention

```
type(scope): subject

body

footer
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Tests
- `chore`: Maintenance

## PR Template

See [references/pr-template.md](references/pr-template.md)

## Scripts

- [scripts/branch-check.sh](scripts/branch-check.sh) - Validate branch name
- [scripts/commit-msg-hook.sh](scripts/commit-msg-hook.sh) - Commit message lint

## Assets

- [assets/branching-model.png](assets/branching-model.png) - Visual branching model
