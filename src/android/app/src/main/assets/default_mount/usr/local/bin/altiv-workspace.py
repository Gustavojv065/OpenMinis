#!/usr/bin/env python3
"""ALTIV local GitHub workspace: clone, inspect, preview, review and approved push.

Static localhost preview is supported for plain HTML projects. Framework apps
require their own build/dev server and are never executed automatically.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.parse

REPO_RE = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
NAME_RE = re.compile(r"^[A-Za-z0-9_.-]+$")
ROOT = Path(os.environ.get("ALTIV_WORKSPACES_DIR", "/root/altiv-workspaces")).resolve()


class WorkspaceError(Exception):
    pass


def run(*args, cwd=None, env=None):
    p = subprocess.run(args, cwd=cwd, env=env, text=True, capture_output=True, timeout=180)
    if p.returncode:
        raise WorkspaceError("Git/comando falhou (%s); revise permissões, branch ou conectividade" % p.returncode)
    return p.stdout.strip()


def workspace(repo):
    if not REPO_RE.fullmatch(repo) or ".." in repo:
        raise WorkspaceError("Repositório inválido")
    owner, name = repo.split("/")
    target = ROOT / (owner + "__" + name)
    if target.exists() and (target.is_symlink() or not target.is_dir()):
        raise WorkspaceError("Caminho de projeto inválido")
    return target


def resolve_workspace(repo, explicit_path=""):
    """Use an existing checkout only when its Git origin matches the requested repo."""
    if not explicit_path:
        return workspace(repo)
    target = Path(explicit_path).expanduser().resolve()
    if not target.is_dir() or not (target / ".git").exists():
        raise WorkspaceError("Diretório informado não é um checkout Git válido")
    origin = run("git", "remote", "get-url", "origin", cwd=target)
    expected = repo.lower()
    parsed = origin.removesuffix(".git").rstrip("/")
    if parsed.startswith("https://github.com/"):
        actual = parsed[len("https://github.com/"):].lower()
    elif parsed.startswith("git@github.com:"):
        actual = parsed[len("git@github.com:"):].lower()
    else:
        raise WorkspaceError("Origem Git não é github.com; workspace recusado")
    if actual != expected:
        raise WorkspaceError("Origem Git não corresponde ao repositório solicitado")
    return target


def git_auth():
    token = os.environ.get("ALTIV_GITHUB_MCP_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise WorkspaceError("GitHub não conectado; autentique a conta no aplicativo")
    tmp = tempfile.TemporaryDirectory(prefix="altiv-git-auth-")
    askpass = Path(tmp.name) / "askpass.sh"
    askpass.write_text('#!/bin/sh\ncase "$1" in *Username*) printf "x-access-token\\n";; *) printf "%s\\n" "$ALTIV_GIT_SECRET";; esac\n')
    askpass.chmod(0o700)
    env = os.environ.copy()
    env.update({"GIT_ASKPASS": str(askpass), "GIT_TERMINAL_PROMPT": "0",
                "ALTIV_GIT_SECRET": token, "GIT_CONFIG_NOSYSTEM": "1"})
    return tmp, env


def remote_fetch(target, branch):
    """Refresh remote refs with ephemeral askpass; never persist the token."""
    if not branch or not NAME_RE.fullmatch(branch.replace("/", "")):
        raise WorkspaceError("Branch inválida")
    tmp, env = git_auth()
    try:
        run("git", "fetch", "--no-tags", "origin", "refs/heads/" + branch + ":refs/remotes/origin/" + branch, cwd=target, env=env)
    finally:
        tmp.cleanup()


def inspect(target):
    if not (target / ".git").exists():
        raise WorkspaceError("Projeto não foi clonado")
    branch = run("git", "branch", "--show-current", cwd=target)
    porcelain = run("git", "status", "--porcelain", cwd=target)
    pkg = target / "package.json"
    framework = "html-static" if (target / "index.html").exists() and not pkg.exists() else "unknown"
    if pkg.exists():
        data = json.loads(pkg.read_text())
        deps = {**data.get("dependencies", {}), **data.get("devDependencies", {})}
        if "@tanstack/react-start" in deps or "@tanstack/start" in deps:
            framework = "tanstack-start"
        elif "next" in deps:
            framework = "nextjs"
        elif "vite" in deps:
            framework = "vite"
        elif "react" in deps:
            framework = "react"
    return {"directory": str(target), "branch": branch, "framework": framework,
            "dirty": bool(porcelain), "changes": porcelain.splitlines()[:60]}


def main():
    p = argparse.ArgumentParser(description="ALTIV: projetos GitHub locais")
    p.add_argument("action", choices=["clone", "status", "diff", "preview", "publish", "sync"])
    p.add_argument("--repo", required=True)
    p.add_argument("--branch", default="")
    p.add_argument("--path", default="", help="Checkout local existente (origem Git validada)")
    p.add_argument("--file", action="append", default=[], help="Arquivo aprovado para publicação (repetível)")
    p.add_argument("--approve", action="store_true", help="Confirma envio ao GitHub")
    p.add_argument("--message", default="feat: alterações revisadas no ALTIV DEV AI")
    p.add_argument("--port", type=int, default=8765)
    a = p.parse_args()
    target = resolve_workspace(a.repo, a.path)
    if a.action == "clone" and a.path:
        raise WorkspaceError("Clone não aceita --path; use --path somente com checkout existente")
    if a.action == "clone":
        if target.exists():
            raise WorkspaceError("Projeto já existe; não sobrescrevi arquivos locais")
        ROOT.mkdir(mode=0o700, parents=True, exist_ok=True)
        tmp, env = git_auth()
        try:
            url = "https://github.com/" + a.repo + ".git"
            args = ["git", "clone", "--no-checkout", "--", url, str(target)]
            run(*args, env=env)
            if a.branch:
                run("git", "checkout", a.branch, cwd=target, env=env)
            else:
                run("git", "checkout", "HEAD", cwd=target, env=env)
            # Never persist credentials in the remote URL.
            run("git", "remote", "set-url", "origin", url, cwd=target)
        except Exception:
            if target.exists():
                import shutil
                shutil.rmtree(target)
            raise
        finally:
            tmp.cleanup()
        print(json.dumps({"status": "cloned", **inspect(target)}, ensure_ascii=False))
        return
    info = inspect(target)
    if a.branch and a.branch != info["branch"]:
        raise WorkspaceError("Branch atual difere da solicitada; interrompido")
    if a.action == "sync":
        remote_fetch(target, info["branch"])
        ahead = run("git", "rev-list", "--left-right", "--count", "HEAD...origin/" + info["branch"], cwd=target)
        print(json.dumps({"status": "synced", "ahead_behind": ahead, **inspect(target)}, ensure_ascii=False))
    elif a.action == "status":
        print(json.dumps(info, ensure_ascii=False))
    elif a.action == "diff":
        changes = run("git", "diff", "--", ".", cwd=target)
        staged = run("git", "diff", "--cached", "--", ".", cwd=target)
        print(json.dumps({"status": "review", "diff": (changes + "\n" + staged)[:60000],
                          **info}, ensure_ascii=False))
    elif a.action == "preview":
        if info["framework"] != "html-static":
            raise WorkspaceError("Preview automático não disponível para " + info["framework"] +
                                 "; é necessário servidor de desenvolvimento compatível")
        if not 1024 <= a.port <= 65535:
            raise WorkspaceError("Porta inválida")
        print("Preview local http://127.0.0.1:%d/ (Ctrl+C para parar)" % a.port, flush=True)
        subprocess.run([sys.executable, "-m", "http.server", str(a.port),
                        "--bind", "127.0.0.1", "--directory", str(target)], check=True)
    elif a.action == "publish":
        if not a.approve:
            raise WorkspaceError("Envio bloqueado: aprovação explícita (--approve) necessária")
        if info["branch"] in ("main", "master") or not info["branch"]:
            raise WorkspaceError("Publicação na branch principal bloqueada; use branch de teste")
        if not info["dirty"]:
            raise WorkspaceError("Nenhuma alteração local para enviar")
        if not a.file:
            raise WorkspaceError("Informe --file para cada arquivo aprovado; stage geral bloqueado")
        approved = []
        for f in a.file:
            rel = Path(f)
            if rel.is_absolute() or ".." in rel.parts or not (target / rel).is_file():
                raise WorkspaceError("Arquivo aprovado inválido: " + f)
            if rel.name.startswith(".env") or rel.name in ("id_rsa", "id_ed25519") or (target / rel).is_symlink():
                raise WorkspaceError("Arquivo sensível ou link simbólico bloqueado: " + f)
            approved.append(str(rel))
        remote_fetch(target, info["branch"])
        if run("git", "rev-parse", "HEAD", cwd=target) != run("git", "rev-parse", "origin/" + info["branch"], cwd=target):
            raise WorkspaceError("Branch remota divergiu; publicação bloqueada antes do stage")
        if run("git", "diff", "--cached", "--name-only", cwd=target):
            raise WorkspaceError("Stage preexistente detectado; revise antes de publicar")
        run("git", "add", "--", *approved, cwd=target)
        staged = run("git", "diff", "--cached", "--name-only", cwd=target).splitlines()
        if not staged or set(staged) != set(approved):
            raise WorkspaceError("Stage não corresponde aos arquivos aprovados")
        run("git", "diff", "--cached", "--check", cwd=target)
        run("git", "-c", "user.name=ALTIV DEV AI", "-c", "user.email=altiv-dev-ai@users.noreply.github.com",
            "commit", "-m", a.message, cwd=target)
        tmp, env = git_auth()
        try:
            run("git", "push", "origin", "HEAD:refs/heads/" + info["branch"], cwd=target, env=env)
        finally:
            tmp.cleanup()
        sha = run("git", "rev-parse", "HEAD", cwd=target)
        print(json.dumps({"status": "pushed", "commit": sha, "branch": info["branch"],
                          "repository": a.repo}))


if __name__ == "__main__":
    try:
        main()
    except (WorkspaceError, subprocess.TimeoutExpired, OSError, ValueError, json.JSONDecodeError) as e:
        print("ALTIV workspace: " + str(e), file=sys.stderr)
        sys.exit(1)
