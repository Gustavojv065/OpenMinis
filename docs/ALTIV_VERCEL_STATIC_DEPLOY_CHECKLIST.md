# ALTIV DEV AI — regra de publicação Vercel para sites estáticos

## Incidente validado (CRS Barbearia, 2026-11-21)
- Repositório: `Gustavojv065/crs-barbearia`
- HTML na raiz (`index.html`), `style.css`, `script.js` e fotos em `public/*.webp`.
- O site funcionava no localhost, mas a URL de produção da Vercel mostrava 404, apesar de deployment Ready.
- Correção no commit `c1ab0c0c70983e71a778711f4e31bc1ab4aabc1d`: criar `vercel.json` na raiz.
- O painel da Vercel mostrou preview correto, status Ready e o commit após a mudança.
- Isto **não prova** que todo erro 404 é causado pela ausência do arquivo; também verificar Root Directory, Output Directory, domínio, proteção e deployment ativo.

## Regra para implementar no APK
Quando o agente criar ou publicar um projeto **HTML/CSS/JS estático sem framework**, com `index.html` na raiz:
1. Identificar o tipo de projeto antes de aplicar qualquer template.
2. Se a publicação de destino for Vercel e **não existir** `vercel.json`, sugerir ou gerar o arquivo abaixo **antes do commit**, com autorização de escrita apropriada.
3. Se já existir `vercel.json`, preservar as configurações existentes; não sobrescrever sem revisão.
4. Para React/Vite, Next.js e outros frameworks, **não aplicar automaticamente** esta configuração: detectar framework, build e pasta de saída correta.
5. Verificar que os caminhos de CSS, JS e imagens correspondem aos arquivos commitados, inclusive maiúsculas/minúsculas.
6. Após o push, verificar deployment e URL de produção; status Ready isoladamente não comprova que a página responde 200.
7. Nunca afirmar que o APK já automatiza isso sem implementação, testes e nova build.

```json
{
  "version": 2,
  "framework": null,
  "buildCommand": null,
  "outputDirectory": "."
}
```

## Critérios de aceite para a futura implementação
- Projeto estático novo recebe configuração correta sem alterar layout ou imagens.
- Projeto com configuração existente não sofre sobrescrita.
- Projeto Vite/Next.js não recebe configuração estática inadequada.
- Diagnóstico diferencia falha de deploy de 404 no domínio de produção.
- Testes automatizados e build Android ARM64 antes da entrega do APK.
