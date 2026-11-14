#!/bin/sh
# product-architect: lint every design root on trunk. Wire this into CI
# (e.g. a Bitbucket Pipelines step: `sh design/ci-lint.sh`). Main holds only
# golden-or-later deltas, so a finding here is a real defect, not WIP.
# One design-lint process lints every root (it walks the repo once and runs the
# repo-wide checks once); every verify marker is re-run here, unlike the
# pre-commit gate, which runs only the markers of the deltas a commit touches.
set -e
LINT="${PA_SKILL_LINT:-}"
if [ -z "$LINT" ]; then for r in "${PA_SKILL_DIR:+$PA_SKILL_DIR/..}" "$HOME/.claude/skills" "$HOME/.agents/skills" "$HOME/.config/opencode/skill" "$HOME/.config/opencode/skills" "$HOME/.codex/skills" ".claude/skills" ".opencode/skill" ".agents/skills"; do [ -f "$r/product-architect/scripts/design-lint.py" ] && { LINT="$r/product-architect/scripts/design-lint.py"; break; }; done; fi
[ -f "$LINT" ] || { echo "design-lint.py not found (set PA_SKILL_LINT)" >&2; exit 1; }
root="$(git rev-parse --show-toplevel)"
python3 "$LINT" --all-roots --repo-root "$root"
