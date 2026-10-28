# US05 — Criar tarefa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 5 | RF10 | US03 |

## User story

**Como** membro de projecto,
**quero** criar uma tarefa no projecto,
**para** registar trabalho que é preciso fazer.

## Regras

| Campo | Regras |
|---|---|
| Título | Obrigatório, entre 1 e 200 caracteres. |
| Descrição | Opcional, até 5000 caracteres. |
| Data limite | Opcional. Tem de ser uma data válida e não pode ser anterior ao dia actual (D12). |

Uma tarefa nova começa sempre no estado "To do" e sem responsável ("Unassigned").

## Critérios de aceitação

O contexto comum aos cenários é que a Ana é membro do projecto "Website" e hoje é 2026-11-02.

### Cenário principal

```gherkin
Scenario: CA01 — Criar tarefa com todos os campos
  When a Ana cria no projecto "Website" uma tarefa com:
    | título      | Desenhar página inicial    |
    | descrição   | Seguir o guia de estilo    |
    | data limite | 2026-11-20                 |
  Then a tarefa "Desenhar página inicial" é criada no projecto "Website" com esses dados
  And o estado da tarefa é "To do"
  And a tarefa não tem responsável ("Unassigned")
  And a tarefa aparece na lista de tarefas do projecto (US06)

Scenario: CA02 — Criar tarefa só com título
  When a Ana cria no projecto "Website" uma tarefa só com o título "Rever textos"
  Then a tarefa "Rever textos" é criada sem descrição e sem data limite
```

### Situações excepcionais

```gherkin
Scenario Outline: CA03 — Título em falta
  When a Ana cria uma tarefa com o título <título>
  Then é apresentada a mensagem "Task title is required."
  And não é criada nenhuma tarefa

  Examples:
    | título |
    | ""     |
    | "   "  |

Scenario: CA04 — Data limite no passado
  When a Ana cria uma tarefa com a data limite 2026-11-01
  Then é apresentada a mensagem "Due date cannot be in the past."
  And não é criada nenhuma tarefa

Scenario: CA05 — Data limite inexistente
  When a Ana cria uma tarefa com a data limite 2027-02-30
  Then é apresentada a mensagem "Enter a valid date."
  And não é criada nenhuma tarefa

Scenario: CA06 — Criar tarefa num projecto de que não sou membro
  Given o Rui não é membro do projecto "Website"
  When o Rui tenta criar uma tarefa no projecto "Website"
  Then é apresentada a mensagem "Project not found."
  And não é criada nenhuma tarefa
```

### Casos-limite

```gherkin
Scenario: CA07 — Data limite igual ao dia actual
  When a Ana cria uma tarefa com a data limite 2026-11-02
  Then a tarefa é criada com a data limite 2026-11-02

Scenario Outline: CA08 — Limites do título e da descrição
  When a Ana cria uma tarefa com um <campo> de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | campo     | n    | resultado                                                  |
    | título    | 200  | a tarefa é criada                                          |
    | título    | 201  | a mensagem "Task title must be at most 200 characters."    |
    | descrição | 5000 | a tarefa é criada                                          |
    | descrição | 5001 | a mensagem "Description must be at most 5000 characters."  |

Scenario: CA09 — Títulos repetidos no mesmo projecto
  Given já existe a tarefa "Rever textos" no projecto "Website"
  When a Ana cria outra tarefa com o título "Rever textos" no mesmo projecto
  Then é criada uma segunda tarefa, distinta da primeira
```
