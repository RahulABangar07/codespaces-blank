#!/bin/bash

# === Configuration ===
REPO_URL="https://gitlab.com/group/repo.git"
CLONE_DIR="temp_repo"

# === Clone the repo (shallow clone) ===
git clone --depth 1 "$REPO_URL" "$CLONE_DIR"
cd "$CLONE_DIR" || exit

# === Get all subfolders ===
mapfile -t folder_array < <(find . -type d ! -path './.git*' ! -path '.')

# === Print the array ===
echo "Folders:"
printf "%s\n" "${folder_array[@]}"

# === Clean up ===
cd ..
rm -rf "$CLONE_DIR"
