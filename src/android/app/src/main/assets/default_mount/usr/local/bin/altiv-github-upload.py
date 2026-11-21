#!/usr/bin/env python3
"""ALTIV GitHub binary publisher. Runs inside the Android Linux workspace.

Credentials are read from the process environment, never arguments or logs.
Use only on repositories and branches explicitly authorized by the user.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

REPO = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
MAX_BYTES = 20 * 1024 * 1024
RETRY_CODES = {429, 500, 502, 503, 504}


class UploadError(Exception):
    pass


def api(method, url, token, payload=None, retries=3):
    body = None if payload is None else json.dumps(payload, separators=(",", ":")).encode()
    for attempt in range(retries):
        req = urllib.request.Request(
            "https://api.github.com" + url, data=body, method=method,
            headers={"Authorization": "Bearer " + token,
                     "Accept": "application/vnd.github+json",
                     "X-GitHub-Api-Version": "2022-11-28",
                     "Content-Type": "application/json", "User-Agent": "ALTIV-DEV-AI"},
        )
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                return json.load(response)
        except urllib.error.HTTPError as exc:
            if exc.code in RETRY_CODES and attempt + 1 < retries:
                time.sleep(min(2 ** attempt, 8))
                continue
            explanations = {401: "Credencial inválida", 403: "Sem permissão ou limite atingido",
                            409: "Conflito de branch", 413: "Arquivo muito grande",
                            422: "Conteúdo ou operação inválida"}
            raise UploadError(explanations.get(exc.code, "Falha GitHub") + " (HTTP %d)" % exc.code) from None
        except (TimeoutError, urllib.error.URLError):
            if attempt + 1 < retries:
                time.sleep(min(2 ** attempt, 8))
                continue
            raise UploadError("Falha de rede; confira o estado remoto antes de repetir") from None


def publish(repo, branch, files, token, message):
    if not REPO.fullmatch(repo) or ".." in repo:
        raise UploadError("Repositório inválido")
    if not branch or branch.startswith("-") or ".." in branch or branch.endswith("/"):
        raise UploadError("Branch inválida")
    root = "/repos/" + repo
    encoded_branch = urllib.parse.quote(branch, safe="")
    ref_path = root + "/git/ref/heads/" + encoded_branch
    ref = api("GET", ref_path, token)
    head = ref["object"]["sha"]
    commit = api("GET", root + "/git/commits/" + head, token)
    entries = []
    expected = {}
    for local, remote in files:
        if not local.is_file() or local.is_symlink():
            raise UploadError("Arquivo local inválido: " + str(local))
        path = Path(remote)
        if path.is_absolute() or not remote or any(p in ("", ".", "..") for p in remote.split("/")) or "\\" in remote:
            raise UploadError("Caminho remoto inválido")
        size = local.stat().st_size
        if size > MAX_BYTES:
            raise UploadError("Arquivo excede limite de 20 MiB: " + remote)
        data = local.read_bytes()
        sha256 = hashlib.sha256(data).hexdigest()
        blob = api("POST", root + "/git/blobs", token, {
            "content": base64.b64encode(data).decode("ascii"), "encoding": "base64"
        })
        entries.append({"path": remote, "mode": "100644", "type": "blob", "sha": blob["sha"]})
        expected[remote] = (blob["sha"], size, sha256)
    tree = api("POST", root + "/git/trees", token, {
        "base_tree": commit["tree"]["sha"], "tree": entries
    })
    if tree["sha"] == commit["tree"]["sha"]:
        return {"status": "unchanged", "commit": head, "files": expected}
    new_commit = api("POST", root + "/git/commits", token, {
        "message": message, "tree": tree["sha"], "parents": [head]
    })
    # Never force: a concurrent writer must not be overwritten.
    api("PATCH", root + "/git/refs/heads/" + encoded_branch, token,
        {"sha": new_commit["sha"], "force": False})
    remote_commit = api("GET", root + "/git/commits/" + new_commit["sha"], token)
    remote_tree = api("GET", root + "/git/trees/" + remote_commit["tree"]["sha"] + "?recursive=1", token)
    found = {entry["path"]: entry for entry in remote_tree["tree"] if entry["type"] == "blob"}
    for path, (sha, size, digest) in expected.items():
        if path not in found or found[path]["sha"] != sha or found[path].get("size") != size:
            raise UploadError("Verificação remota falhou: " + path)
    return {"status": "committed", "commit": new_commit["sha"], "files": expected}


def main():
    parser = argparse.ArgumentParser(description="Publicar arquivos binários no GitHub")
    parser.add_argument("--repo", required=True)
    parser.add_argument("--branch", required=True)
    parser.add_argument("--file", action="append", nargs=2, metavar=("LOCAL", "REMOTE"), required=True)
    parser.add_argument("--message", default="ALTIV: publicar arquivos binários")
    args = parser.parse_args()
    token = os.environ.get("ALTIV_GITHUB_MCP_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise UploadError("Configure ALTIV_GITHUB_MCP_TOKEN no ambiente protegido")
    result = publish(args.repo, args.branch, [(Path(a), b) for a, b in args.file], token, args.message)
    print(json.dumps({"status": result["status"], "commit": result["commit"],
                      "files": {p: {"size": v[1], "sha256": v[2]} for p, v in result["files"].items()}}))


if __name__ == "__main__":
    try:
        main()
    except UploadError as exc:
        print("ALTIV GitHub: " + str(exc), file=sys.stderr)
        sys.exit(1)
