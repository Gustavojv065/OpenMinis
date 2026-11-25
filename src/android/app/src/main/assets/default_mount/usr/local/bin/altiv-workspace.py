#!/usr/bin/env python3
"""ALTIV local GitHub workspace: clone, inspect, preview, review and approved push.

Static localhost preview is supported for plain HTML projects. Framework apps
require their own build/dev server and are never executed automatically.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import urllib.error

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
    target = Path(explicit_path).expanduser().resolve() if explicit_path else workspace(repo)
    if not explicit_path and not target.exists():
        return target
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


def list_repositories():
    """List only repositories accessible to the configured GitHub credential."""
    token = os.environ.get("ALTIV_GITHUB_MCP_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise WorkspaceError("GitHub não conectado; autentique a conta no aplicativo")
    repositories = []
    seen = set()
    try:
        req = urllib.request.Request("https://api.github.com/user", headers={"Authorization": "Bearer " + token, "Accept": "application/vnd.github+json"})
        with urllib.request.urlopen(req, timeout=20) as response:
            account = json.load(response).get("login")
        if not account:
            raise WorkspaceError("Identidade GitHub não confirmada")
        for page in range(1, 101):
            url = "https://api.github.com/user/repos?per_page=30&type=all&sort=full_name&page=" + str(page)
            req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token,
                "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"})
            with urllib.request.urlopen(req, timeout=20) as response:
                entries = json.load(response)
            if not isinstance(entries, list):
                raise WorkspaceError("Resposta inesperada do GitHub")
            for x in entries:
                if not isinstance(x, dict) or not x.get("full_name") or not isinstance(x.get("private"), bool):
                    raise WorkspaceError("Metadados de repositório incompletos")
                key = x.get("id") or x["full_name"].lower()
                if key not in seen:
                    seen.add(key)
                    repositories.append({"full_name": x["full_name"], "private": x["private"]})
            if len(entries) < 30:
                return {"status": "listed", "account": account, "complete": True, "repositories": repositories}
        return {"status": "listed", "account": account, "complete": False, "repositories": repositories}
    except urllib.error.HTTPError as e:
        raise WorkspaceError("GitHub recusou a consulta (HTTP %d); confira permissões" % e.code) from None
    except (urllib.error.URLError, TimeoutError):
        raise WorkspaceError("API GitHub inacessível; verifique DNS ou use MCP autenticado") from None


def remote_fetch(target, branch):
    """Refresh remote refs with ephemeral askpass; never persist the token."""
    if not branch or not re.fullmatch(r"[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*", branch) or ".." in branch:
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


def verify_approved_content(target, approved):
    """Reject accidental shell-command replacement and destructive shrinkage."""
    for name in approved:
        path = target / name
        data = path.read_bytes()
        if not data:
            raise WorkspaceError("Arquivo vazio bloqueado: " + name)
        if path.suffix in (".tsx", ".ts", ".jsx", ".js", ".py", ".html"):
            stripped = data.strip()
            if len(stripped) < 160 and (stripped.startswith((b"git ", b"cat ", b"sed ", b"python ", b"curl ", b"bash ", b"sh ")) or b"git push" in stripped):
                raise WorkspaceError("Comando suspeito substituindo código: " + name)
            exists = subprocess.run(["git", "cat-file", "-e", "HEAD:" + name],
                                    cwd=target, capture_output=True, timeout=30).returncode == 0
            if exists:
                previous_size = len(subprocess.run(["git", "show", "HEAD:" + name],
                                    cwd=target, capture_output=True, timeout=30, check=True).stdout)
                if previous_size > 2000 and len(data) < previous_size // 3:
                    raise WorkspaceError("Exclusão massiva bloqueada: " + name)


def diagnose(target, branch):
    """User-facing, secret-free diagnostics; never modifies the remote."""
    result = {"status": "diagnostic", "branch": branch, "checks": []}
    def check(code, ok, message, correction):
        result["checks"].append({"code": code, "ok": ok,
                                 "message": message, "suggested_fix": correction})
    token_present = bool(os.environ.get("ALTIV_GITHUB_MCP_TOKEN") or os.environ.get("GITHUB_TOKEN"))
    check("github_git_credentials", token_present,
          "Credencial Git disponível para o workspace" if token_present else
          "O Git do workspace não recebeu uma credencial do GitHub",
          "Reconectar GitHub nas configurações do ALTIV ou usar o conector GitHub autenticado; nunca pedir o token no chat")
    check("workspace", (target / ".git").exists(), "Workspace Git identificado",
          "Clonar novamente o projeto sem apagar alterações locais")
    if (target / ".git").exists():
        local_sha = run("git", "rev-parse", "HEAD", cwd=target)
        result["local_commit"] = local_sha
        if token_present and branch:
            try:
                remote_fetch(target, branch)
                remote_sha = run("git", "rev-parse", "origin/" + branch, cwd=target)
                result["remote_commit"] = remote_sha
                check("remote_sync", remote_sha == local_sha,
                      "Referências locais e remotas " + ("coincidem" if remote_sha == local_sha else "divergem"),
                      "Revisar divergência antes de publicar; nunca sobrescrever a branch remota")
            except (WorkspaceError, subprocess.TimeoutExpired) :
                check("remote_sync", False, "Não foi possível consultar a branch remota",
                      "Verificar conexão, permissão de leitura e credencial GitHub; tentar novamente sem publicar")
        else:
            check("remote_sync", False, "Consulta remota não executada sem credencial Git",
                  "Conectar o GitHub para permitir verificação remota segura")
    result["ready_for_publish"] = all(c["ok"] for c in result["checks"])
    return result


def publication_plan(target, repo, branch, approved):
    """Read-only publication handoff for Git or authenticated MCP tools.

    No credentials or file contents are exposed. MCP agents must read the exact
    local bytes, verify the remote head, obtain approval and verify remote blobs.
    """
    if branch in ("main", "master", ""):
        raise WorkspaceError("Use uma branch isolada; publicação na principal bloqueada")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*", branch) or ".." in branch:
        raise WorkspaceError("Branch inválida")
    if not approved:
        raise WorkspaceError("Selecione os arquivos aprovados")
    if run("git", "diff", "--cached", "--name-only", cwd=target):
        raise WorkspaceError("Stage preexistente; revise antes de continuar")
    changed = set(run("git", "diff", "--name-only", cwd=target).splitlines())
    untracked = set(run("git", "ls-files", "--others", "--exclude-standard", cwd=target).splitlines())
    selected = set(approved)
    if changed | untracked != selected:
        raise WorkspaceError("Arquivos modificados não correspondem à seleção aprovada")
    files = []
    for name in sorted(selected):
        rel = Path(name)
        path = target / rel
        if rel.is_absolute() or ".." in rel.parts or not path.is_file() or path.is_symlink():
            raise WorkspaceError("Arquivo inválido: " + name)
        if rel.name.startswith(".env") or rel.name in ("id_rsa", "id_ed25519"):
            raise WorkspaceError("Arquivo sensível bloqueado: " + name)
        verify_approved_content(target, [name])
        data = path.read_bytes()
        files.append({"path": name, "size_bytes": len(data),
                      "sha256": hashlib.sha256(data).hexdigest(),
                      "git_blob_sha": hashlib.sha1(b"blob " + str(len(data)).encode() + b"\\0" + data).hexdigest()})
    run("git", "diff", "--check", cwd=target)
    return {"status": "approval_required", "repository": repo, "target_branch": branch,
            "local_base_commit": run("git", "rev-parse", "HEAD", cwd=target),
            "files": files, "approved": False,
            "methods": {"git": bool(os.environ.get("ALTIV_GITHUB_MCP_TOKEN") or os.environ.get("GITHUB_TOKEN")),
                        "mcp": "requires authenticated write tool and permission verification"},
            "next_steps": [
                "Consultar SHA da main remota com conector autenticado; nunca confiar somente em origin/main local",
                "Confirmar permissão real de escrita do conector antes de qualquer alteração",
                "Criar branch remota isolada a partir do SHA remoto aprovado",
                "Solicitar aprovação explícita do usuário para os arquivos e o destino",
                "Publicar bytes exatos dos arquivos aprovados, sem comandos no lugar do conteúdo",
                "Conferir SHA dos blobs remotos e abrir Pull Request sem merge automático"]}


def main():
    p = argparse.ArgumentParser(description="ALTIV: projetos GitHub locais")
    p.add_argument("action", choices=["clone", "status", "diff", "preview", "publish", "sync", "diagnose", "publish-plan", "list"])
    p.add_argument("--repo", default="")
    p.add_argument("--branch", default="")
    p.add_argument("--path", default="", help="Checkout local existente (origem Git validada)")
    p.add_argument("--file", action="append", default=[], help="Arquivo aprovado para publicação (repetível)")
    p.add_argument("--approve", action="store_true", help="Confirma envio ao GitHub")
    p.add_argument("--message", default="feat: alterações revisadas no ALTIV DEV AI")
    p.add_argument("--port", type=int, default=8765)
    a = p.parse_args()
    if a.action == "list":
        print(json.dumps(list_repositories(), ensure_ascii=False))
        return
    if not REPO_RE.fullmatch(a.repo) or ".." in a.repo:
        raise WorkspaceError("Informe o repositório exato dono/nome")
    target = resolve_workspace(a.repo, a.path)
    if a.action == "clone" and a.path:
        raise WorkspaceError("Clone não aceita --path; use --path somente com checkout existente")
    if a.action == "clone":
        if target.exists():
            print(json.dumps({"status": "reused", "repository": a.repo, **inspect(target)}, ensure_ascii=False))
            return
        ROOT.mkdir(mode=0o700, parents=True, exist_ok=True)
        tmp, env = git_auth()
        try:
            url = "https://github.com/" + a.repo + ".git"
            args = ["git", "clone", "--no-checkout", "--", url, str(target)]
            run(*args, env=env)
            if a.branch:
                run("git", "checkout", a.branch, cwd=target, env=env)
            else:
                run("git", "reset", "--hard", "HEAD", cwd=target, env=env)
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
    if a.action == "publish-plan":
        print(json.dumps(publication_plan(target, a.repo, info["branch"], a.file), ensure_ascii=False))
    elif a.action == "diagnose":
        print(json.dumps(diagnose(target, info["branch"]), ensure_ascii=False))
    elif a.action == "sync":
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
        verify_approved_content(target, approved)
        publication_plan(target, a.repo, info["branch"], approved)
        tmp, env = git_auth()
        try:
            refs = run("git", "ls-remote", "--heads", "origin", "refs/heads/" + info["branch"], cwd=target, env=env)
        finally:
            tmp.cleanup()
        if refs:
            remote_fetch(target, info["branch"])
        else:
            remote_fetch(target, "main")
        base_ref = "origin/" + (info["branch"] if refs else "main")
        if run("git", "rev-parse", "HEAD", cwd=target) != run("git", "rev-parse", base_ref, cwd=target):
            raise WorkspaceError("Branch remota divergiu; publicação bloqueada antes do stage")
        if run("git", "diff", "--cached", "--name-only", cwd=target):
            raise WorkspaceError("Stage preexistente detectado; revise antes de publicar")
        run("git", "add", "--", *approved, cwd=target)
        staged = run("git", "diff", "--cached", "--name-only", cwd=target).splitlines()
        if not staged or set(staged) != set(approved):
            raise WorkspaceError("Stage não corresponde aos arquivos aprovados")
        run("git", "diff", "--cached", "--check", cwd=target)
        # Verify the exact approved patch before creating a commit; never stage other files.
        staged_patch = run("git", "diff", "--cached", "--binary", "--", *approved, cwd=target)
        if not staged_patch:
            raise WorkspaceError("Diff aprovado vazio; commit bloqueado")
        run("git", "-c", "user.name=ALTIV DEV AI", "-c", "user.email=altiv-dev-ai@users.noreply.github.com",
            "commit", "-m", a.message, cwd=target)
        tmp, env = git_auth()
        try:
            run("git", "push", "origin", "HEAD:refs/heads/" + info["branch"], cwd=target, env=env)
        finally:
            tmp.cleanup()
        sha = run("git", "rev-parse", "HEAD", cwd=target)
        # A successful command is not enough: confirm the remote ref points to our SHA.
        remote_fetch(target, info["branch"])
        remote_sha = run("git", "rev-parse", "origin/" + info["branch"], cwd=target)
        if remote_sha != sha:
            raise WorkspaceError("Push não confirmado: SHA remoto diferente do commit local")
        for name in approved:
            local_blob = run("git", "rev-parse", "HEAD:" + name, cwd=target)
            remote_blob = run("git", "rev-parse", "origin/" + info["branch"] + ":" + name, cwd=target)
            if local_blob != remote_blob:
                raise WorkspaceError("Arquivo remoto diferente do aprovado: " + name)
        print(json.dumps({"status": "pushed", "commit": sha, "branch": info["branch"],
                          "repository": a.repo}))


if __name__ == "__main__":
    try:
        main()
    except (WorkspaceError, subprocess.TimeoutExpired, OSError, ValueError, json.JSONDecodeError) as e:
        print("ALTIV workspace: " + str(e), file=sys.stderr)
        sys.exit(1)
