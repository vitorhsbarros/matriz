# US12 — Gerir os membros de uma equipa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 12 | RF09 | US11 |

## User story

**Como** gestor de equipa,
**quero** adicionar e remover pessoas da equipa,
**para** que a equipa reflicta quem trabalha em conjunto.

## Regras

- As pessoas são adicionadas pelo email da sua conta (D04).
- Só o gestor da equipa pode adicionar e remover membros.
- O gestor não pode ser removido (D07).
- Alterar os membros de uma equipa não desfaz as alocações de tarefas já feitas a essa equipa (D09).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana é gestora da equipa "Design" e existe uma conta do Rui com o email "rui@example.com".

### Cenário principal

```gherkin
Scenario: CA01 — Adicionar um membro
  Given o Rui não é membro da equipa "Design"
  When a Ana adiciona o email "rui@example.com" à equipa "Design"
  Then o Rui passa a ser membro da equipa "Design"
  And a lista de equipas mostra o Rui como membro da equipa "Design"

Scenario: CA02 — Remover um membro
  Given o Rui é membro da equipa "Design"
  When a Ana remove o Rui da equipa "Design"
  Then o Rui deixa de ser membro da equipa "Design"
```

### Situações excepcionais

```gherkin
Scenario: CA03 — Email sem conta
  When a Ana adiciona o email "marta@example.com", que não tem conta
  Then é apresentada a mensagem "No user found with this email."
  And os membros da equipa não mudam

Scenario: CA04 — Pessoa que já é membro
  Given o Rui já é membro da equipa "Design"
  When a Ana adiciona o email "rui@example.com"
  Then é apresentada a mensagem "This user is already a member of the team."
  And os membros da equipa não mudam

Scenario: CA05 — Tentar remover o gestor
  When a Ana tenta remover-se a si própria da equipa "Design"
  Then é apresentada a mensagem "The team manager cannot be removed."
  And a Ana continua gestora da equipa

Scenario: CA06 — Membro que não é gestor tenta gerir membros
  Given o Rui é membro, mas não gestor, da equipa "Design"
  When o Rui tenta adicionar ou remover um membro
  Then é apresentada a mensagem "Only the team manager can manage members."
  And os membros da equipa não mudam
```

### Casos-limite

```gherkin
Scenario: CA07 — Email com maiúsculas diferentes (D03)
  When a Ana adiciona o email "RUI@Example.com"
  Then o Rui passa a ser membro da equipa "Design"

Scenario: CA08 — Alterar a equipa não desfaz alocações existentes (D09)
  Given a tarefa "Criar ícones" do projecto "Website" está alocada à equipa "Design"
  And a Marta não é membro do projecto "Website"
  When a Ana adiciona a Marta à equipa "Design"
  Then a tarefa "Criar ícones" continua alocada à equipa "Design"
  And a Marta continua sem acesso ao projecto "Website"
```
