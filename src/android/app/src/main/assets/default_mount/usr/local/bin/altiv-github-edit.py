#!/usr/bin/env python3
"""ALTIV safe exact-text GitHub editor for authorized repositories.

No blind whole-file overwrite: GitHub Contents API SHA is an optimistic
concurrency guard. The tool changes only an exact, uniquely identified span.
"""
import argparse
import base64
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

REPO = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
BRANCH = re.compile(r"^[A-Za-z0-9_.][A-Za-z0-9_./-]*$")
MAX_BYTES = 1024 * 1024


class EditError(Exception):
    pass


def api(method, url, token, payload=None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        "https://api.github.com" + url, data=data, method=method,
        headers={"Authorization": "Bearer " + token,
                 "Accept": "application/vnd.github+json",
                 "X-GitHub-Api-Version": "2022-11-28",
                 "Content-Type": "application/json",
                 "User-Agent": "ALTIV-DEV-AI"},
    )
    try:
        with urllib.request.urlopen(request, timeout=45) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code in (409, 422):
            raise EditError("Conflito ou arquivo alterado remotamente; releia antes de tentar") from None
        raise EditError("GitHub HTTP %d; confirme token, permissões e caminho" % error.code) from None
    except (TimeoutError, urllib.error.URLError):
        raise EditError("Falha de rede; verifique GitHub antes de repetir") from None


def edit(repo, branch, path, old, new, count, token, message, apply=False):
    if not REPO.fullmatch(repo) or ".." in repo:
        raise EditError("Repositório inválido")
    if not BRANCH.fullmatch(branch) or ".." in branch or branch.endswith("/") or "//" in branch:
        raise EditError("Branch inválida")
    if (not path or path.startswith("/") or "\\" in path or
            any(part in ("", ".", "..") for part in path.split("/"))):
        raise EditError("Caminho inválido")
    if not old or old == new or count < 1:
        raise EditError("Trecho antigo/novo ou contagem inválidos")
    endpoint = ("/repos/" + repo + "/contents/" +
                urllib.parse.quote(path, safe="/") + "?ref=" +
                urllib.parse.quote(branch, safe=""))
    obj = api("GET", endpoint, token)
    if obj.get("type") != "file" or obj.get("size", MAX_BYTES + 1) > MAX_BYTES:
        raise EditError("Apenas arquivos de texto de até 1 MiB são aceitos")
    try:
        raw = base64.b64decode(obj["content"], validate=False)
        original = raw.decode("utf-8")
    except (ValueError, UnicodeDecodeError, KeyError):
        raise EditError("Arquivo não é texto UTF-8") from None
    occurrences = original.count(old)
    if occurrences != count:
        raise EditError("Trecho encontrado %d vez(es), esperado %d; nenhuma mudança" %
                        (occurrences, count))
    updated = original.replace(old, new, count)
    if len(updated.encode("utf-8")) > MAX_BYTES:
        raise EditError("Arquivo atualizado excede 1 MiB")
    if not apply:
        return {"status": "preview", "path": path, "matches": count,
                "original_sha": obj["sha"]}
    write_url = endpoint.split("?")[0]
    response = api("PUT", write_url, token, {
        "message": message,
        "content": base64.b64encode(updated.encode("utf-8")).decode("ascii"),
        "sha": obj["sha"], "branch": branch,
    })
    # Read back the same branch; a successful API response alone is insufficient.
    check = api("GET", endpoint, token)
    try:
        actual = base64.b64decode(check["content"]).decode("utf-8")
    except (KeyError, ValueError, UnicodeDecodeError):
        raise EditError("Não foi possível verificar o conteúdo remoto") from None
    if actual != updated or check["sha"] != response["content"]["sha"]:
        raise EditError("Verificação remota falhou; inspecione o commit")
    return {"status": "committed", "path": path, "matches": count,
            "commit": response["commit"]["sha"]}


def main():
    parser = argparse.ArgumentParser(description="Alteração pontual segura no GitHub")
    parser.add_argument("--repo", required=True)
    parser.add_argument("--branch", required=True)
    parser.add_argument("--path", required=True)
    parser.add_argument("--old", required=True, help="Texto exato a substituir")
    parser.add_argument("--new", required=True, help="Texto de substituição")
    parser.add_argument("--count", type=int, default=1)
    parser.add_argument("--message", default="fix: ajuste pontual via ALTIV DEV AI")
    parser.add_argument("--apply", action="store_true",
                        help="Sem esta opção, somente valida e pré-visualiza")
    args = parser.parse_args()
    token = os.getenv("ALTIV_GITHUB_MCP_TOKEN") or os.getenv("GITHUB_TOKEN")
    if not token:
        raise EditError("Conecte o GitHub ao ALTIV DEV AI")
    print(json.dumps(edit(args.repo, args.branch, args.path, args.old, args.new,
                          args.count, token, args.message, args.apply)))


if __name__ == "__main__":
    try:
        main()
    except EditError as error:
        print("ALTIV GitHub: " + str(error), file=sys.stderr)
        sys.exit(1)
