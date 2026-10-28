# US09 — Gerir os membros de um projecto

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 9 | RF06, RNF03 | US03 |

## User story

**Como** gestor de projecto,
**quero** adicionar e remover membros do projecto,
**para** que as pessoas certas possam trabalhar nele.

## Regras

- Os membros são adicionados pelo email da sua conta (D04).
- Todos os membros vêem a lista de membros do projecto, com o nome, o email e o papel ("Manager" ou "Member") de cada um.
- Só o gestor pode adicionar e remover membros.
- O gestor não pode ser removido (D07).
- Quando um membro é removido, as tarefas alocadas a essa pessoa no projecto ficam sem responsável. As tarefas alocadas a equipas não mudam (D10).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana é gestora do projecto "Website" e existe uma conta do Rui com o email "rui@example.com".

### Cenário principal

```gherkin
Scenario: CA01 — Adicionar um membro
  Given o Rui não é membro do projecto "Website"
  When a Ana adiciona o email "rui@example.com" ao projecto "Website"
  Then o Rui passa a ser membro do projecto, com o papel "Member"
  And o projecto "Website" aparece na lista de projectos do Rui

Scenario: CA02 — Remover um membro
  Given o Rui é membro do projecto "Website"
  When a Ana remove o Rui do projecto "Website"
  Then o Rui deixa de ser membro do projecto
  And o projecto "Website" deixa de aparecer na lista de projectos do Rui

Scenario: CA03 — Tarefas do membro removido ficam sem responsável (D10)
  Given o Rui é membro do projecto "Website" e da equipa "Design"
  And a tarefa "Criar logótipo" está alocada ao Rui
  And a tarefa "Criar ícones" está alocada à equipa "Design"
  When a Ana remove o Rui do projecto "Website"
  Then a tarefa "Criar logótipo" fica sem responsável ("Unassigned")
  And a tarefa "Criar ícones" continua alocada à equipa "Design"
```

### Situações excepcionais

```gherkin
Scenario: CA04 — Email sem conta
  When a Ana adiciona o email "marta@example.com", que não tem conta
  Then é apresentada a mensagem "No user found with this email."
  And os membros do projecto não mudam

Scenario: CA05 — Pessoa que já é membro
  Given o Rui já é membro do projecto "Website"
  When a Ana adiciona o email "rui@example.com"
  Then é apresentada a mensagem "This user is already a member of the project."
  And os membros do projecto não mudam

Scenario: CA06 — Tentar remover o gestor
  When a Ana tenta remover-se a si própria do projecto "Website"
  Then é apresentada a mensagem "The project manager cannot be removed."
  And a Ana continua gestora do projecto

Scenario: CA07 — Membro que não é gestor tenta gerir membros (RNF03)
  Given o Rui é membro, mas não gestor, do projecto "Website"
  When o Rui tenta adicionar ou remover um membro
  Then é apresentada a mensagem "Only the project manager can manage members."
  And os membros do projecto não mudam
```

### Casos-limite

```gherkin
Scenario: CA08 — Email com maiúsculas diferentes (D03)
  When a Ana adiciona o email "RUI@Example.com"
  Then o Rui passa a ser membro do projecto

Scenario: CA09 — Membro removido e adicionado de novo
  Given o Rui foi removido do projecto "Website" e a tarefa "Criar logótipo" ficou sem responsável
  When a Ana volta a adicionar o Rui ao projecto
  Then o Rui volta a ser membro do projecto
  And a tarefa "Criar logótipo" continua sem responsável
```
