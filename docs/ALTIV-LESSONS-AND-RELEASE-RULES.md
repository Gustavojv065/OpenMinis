# ALTIV DEV AI — Memória técnica e regras de prevenção

Regras versionadas a aplicar em todas as próximas versões.

1. Upload de imagens: usar bytes/base64 e API Git Data, nunca UTF-8 para WebP/PNG; conferir SHA remoto.
2. Vercel 404 apesar de Ready: revisar diretório raiz, framework/output e domínio; testar URL pública.
3. Nunca sobrescrever arquivo inteiro com conteúdo remoto incompleto; usar cópia local e diff ou substituição exata com contagem.
4. Se busca encontra 8 ou 10 ocorrências e era esperada 1, interromper; inspecionar contexto antes de editar.
5. CSS textual pode usar gradiente e background-clip; validar cor computada no DOM e screenshot mobile/desktop.
6. Clonar branch de teste e preservar modificações locais. Não executar scripts/deploy/migrações sem inspeção e autorização.
7. Nunca revelar tokens nem persistir token na URL Git; usar GIT_ASKPASS efêmero; bloquear .env e chaves privadas.
8. Para git fetch/push usar mecanismo autenticado do workspace; não usar git fetch direto sem credenciais.
9. Publicar apenas com aprovação explícita, stage seletivo de arquivos autorizados e comparação HEAD/remoto.
10. Não incluir src/routeTree.gen.ts ou outros arquivos preexistentes sem autorização explícita.
11. Bloquear publicação em main/master, stage preexistente, divergência remota e arquivos sensíveis.
12. Maven Central HTTP 429: retry com backoff, sem desabilitar testes; confirmar artefato ARM64.
13. Localhost HTTP 200 não prova todas as rotas, formulários ou integrações Supabase; testar separadamente.
14. Commit em branch de teste não garante atualização do Lovable nem deploy Vercel; validar destino.

## Fluxo padrão
Identificar repo/branch → clone autenticado → inspecionar scripts/ambiente → localhost → edição pontual → diff → validação visual → aprovação → sync autenticado → stage só de arquivos aprovados → git diff --cached --check → commit → push branch de teste → conferir SHA remoto.

## Critérios de release
Verificar presença do utilitário no APK, --help, sync autenticado, publish sem aprovação bloqueado, stage seletivo, segredos bloqueados, branch divergente bloqueada, GitHub Actions verde e artefato ARM64 gerado.

Este documento é um contrato de engenharia e não substitui testes de execução no dispositivo.