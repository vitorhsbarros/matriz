# US11 — Criar equipa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 11 | RF08 | US02 |

## User story

**Como** utilizador,
**quero** criar uma equipa,
**para** poder alocar tarefas a um grupo de pessoas em vez de a uma só.

## Regras

| Campo | Regras |
|---|---|
| Nome | Obrigatório, entre 1 e 50 caracteres. É único entre todas as equipas, sem distinguir maiúsculas de minúsculas (D06). |

- Quem cria a equipa fica como gestor e membro da equipa (D07).
- Todos os utilizadores vêem a lista de equipas, com o nome e os membros de cada uma (D05).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana tem sessão iniciada.

### Cenário principal

```gherkin
Scenario: CA01 — Criar uma equipa
  Given não existe nenhuma equipa chamada "Design"
  When a Ana cria a equipa "Design"
  Then a equipa "Design" é criada
  And a Ana fica como gestora e membro da equipa
  And a equipa "Design" aparece na lista de equipas de todos os utilizadores
```

### Situações excepcionais

```gherkin
Scenario Outline: CA02 — Nome em falta
  When a Ana cria uma equipa com o nome <nome>
  Then é apresentada a mensagem "Team name is required."
  And não é criada nenhuma equipa

  Examples:
    | nome  |
    | ""    |
    | "   " |

Scenario: CA03 — Nome já usado
  Given já existe a equipa "Design"
  When a Ana cria uma equipa com o nome "Design"
  Then é apresentada a mensagem "A team with this name already exists."
  And não é criada nenhuma equipa
```

### Casos-limite

```gherkin
Scenario: CA04 — Nome já usado com maiúsculas e espaços diferentes
  Given já existe a equipa "Design"
  When a Ana cria uma equipa com o nome "  DESIGN  "
  Then é apresentada a mensagem "A team with this name already exists."

Scenario Outline: CA05 — Limites do nome
  When a Ana cria uma equipa com um nome de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | n  | resultado                                                |
    | 1  | a equipa é criada                                        |
    | 50 | a equipa é criada                                        |
    | 51 | a mensagem "Team name must be at most 50 characters."    |
```
