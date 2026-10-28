# US04 — Ver os meus projectos

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 4 | RF07, RNF03 | US03 |

## User story

**Como** utilizador,
**quero** ver a lista dos projectos de que sou membro,
**para** chegar rapidamente ao trabalho de cada um.

## Regras

- Para cada projecto, a lista mostra o nome e o papel do utilizador: "Manager" ou "Member".
- A lista está ordenada alfabeticamente pelo nome, sem distinguir maiúsculas de minúsculas.

## Critérios de aceitação

### Cenário principal

```gherkin
Scenario: CA01 — Ver os projectos de que sou membro
  Given a Ana é gestora do projecto "Website"
  And a Ana é membro do projecto "App"
  And existe o projecto "Loja", de que a Ana não é membro
  When a Ana abre a lista de projectos
  Then a lista mostra, por esta ordem:
    | projecto | papel   |
    | App      | Member  |
    | Website  | Manager |
  And o projecto "Loja" não aparece

Scenario: CA02 — Abrir um projecto da lista
  Given o projecto "Website" aparece na lista de projectos da Ana
  When a Ana abre o projecto "Website"
  Then é apresentada a lista de tarefas do projecto "Website" (US06)
```

### Situações excepcionais

```gherkin
Scenario: CA03 — Não sou membro de nenhum projecto
  Given a Ana não é membro de nenhum projecto
  When abre a lista de projectos
  Then é apresentada a mensagem "You are not a member of any project yet."

Scenario: CA04 — Fui removido de um projecto
  Given a Ana era membro do projecto "App"
  And o gestor removeu a Ana do projecto "App" (US09)
  When a Ana abre a lista de projectos
  Then o projecto "App" não aparece
  And se a Ana tentar abrir o projecto "App" directamente é apresentada a mensagem "Project not found."
```

### Casos-limite

```gherkin
Scenario: CA05 — Ordenação sem distinguir maiúsculas
  Given a Ana é membro dos projectos "beta", "Alpha" e "Gamma"
  When abre a lista de projectos
  Then a ordem apresentada é "Alpha", "beta", "Gamma"

Scenario: CA06 — Projectos com o mesmo nome
  Given a Ana é membro de dois projectos diferentes chamados "Website"
  When abre a lista de projectos
  Then aparecem duas entradas "Website"
  And cada uma abre o seu próprio projecto
```
