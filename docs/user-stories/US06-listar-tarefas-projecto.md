# US06 — Listar as tarefas de um projecto

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 6 | RF15 | US05 |

## User story

**Como** membro de projecto,
**quero** ver todas as tarefas do projecto com o estado e o responsável de cada uma,
**para** ter uma visão geral do trabalho e do seu progresso.

## Regras

- Cada tarefa mostra o título, o estado, o responsável e a data limite (se tiver).
- O responsável aparece como:
  - o nome da pessoa;
  - o nome da equipa, seguido de "(team)";
  - "Unassigned", se não houver responsável.
- As tarefas estão ordenadas da mais recente para a mais antiga, pela data de criação.
- A lista mostra todas as tarefas do projecto, incluindo as concluídas.

## Critérios de aceitação

O contexto comum aos cenários é que a Ana é membro do projecto "Website".

### Cenário principal

```gherkin
Scenario: CA01 — Ver as tarefas do projecto
  Given o projecto "Website" tem estas tarefas, criadas por esta ordem:
    | título         | estado      | responsável | data limite |
    | Rever textos   | Done        | Rui Costa   |             |
    | Criar logótipo | In progress | Design      | 2026-11-20  |
    | Testar o site  | To do       |             |             |
  And "Design" é uma equipa
  When a Ana abre o projecto "Website"
  Then a lista mostra, por esta ordem:
    | título         | estado      | responsável    | data limite |
    | Testar o site  | To do       | Unassigned     |             |
    | Criar logótipo | In progress | Design (team)  | 2026-11-20  |
    | Rever textos   | Done        | Rui Costa      |             |
```

### Situações excepcionais

```gherkin
Scenario: CA02 — Projecto sem tarefas
  Given o projecto "Website" não tem tarefas
  When a Ana abre o projecto "Website"
  Then é apresentada a mensagem "No tasks yet."

Scenario: CA03 — Projecto de que não sou membro
  Given o Rui não é membro do projecto "Website"
  When o Rui tenta ver as tarefas do projecto "Website"
  Then é apresentada a mensagem "Project not found."
  And nenhuma tarefa é mostrada
```

### Casos-limite

```gherkin
Scenario: CA04 — Só aparecem as tarefas do projecto aberto
  Given a Ana é membro dos projectos "Website" e "App"
  And o projecto "App" tem a tarefa "Publicar na loja"
  When a Ana abre o projecto "Website"
  Then a tarefa "Publicar na loja" não aparece

Scenario: CA05 — A lista reflecte alterações de outros membros (RNF07)
  Given o Rui alterou o estado da tarefa "Testar o site" para "In progress"
  When a Ana abre o projecto "Website" depois dessa alteração
  Then a tarefa "Testar o site" aparece com o estado "In progress"
```
