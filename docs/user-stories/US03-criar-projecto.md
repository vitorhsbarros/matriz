# US03 — Criar projecto

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 3 | RF03 | US02 |

## User story

**Como** utilizador,
**quero** criar um projecto com nome e descrição,
**para** reunir num só sítio as tarefas de um trabalho.

## Regras

| Campo | Regras |
|---|---|
| Nome | Obrigatório, entre 1 e 100 caracteres. Pode repetir-se (D06). |
| Descrição | Opcional, até 2000 caracteres. |

Quem cria o projecto fica como gestor e membro do projecto (D07).

## Critérios de aceitação

### Cenário principal

```gherkin
Scenario: CA01 — Criar projecto com nome e descrição
  Given a Ana tem sessão iniciada
  When cria um projecto com o nome "Website" e a descrição "Novo site da empresa"
  Then o projecto "Website" é criado com a descrição "Novo site da empresa"
  And a Ana fica como gestora e membro do projecto
  And o projecto não tem tarefas
  And o projecto "Website" aparece na lista de projectos da Ana

Scenario: CA02 — Criar projecto sem descrição
  Given a Ana tem sessão iniciada
  When cria um projecto com o nome "Website" e sem descrição
  Then o projecto "Website" é criado com a descrição vazia
```

### Situações excepcionais

```gherkin
Scenario Outline: CA03 — Nome em falta
  When a Ana cria um projecto com o nome <nome>
  Then é apresentada a mensagem "Project name is required."
  And não é criado nenhum projecto

  Examples:
    | nome  |
    | ""    |
    | "   " |
```

### Casos-limite

```gherkin
Scenario Outline: CA04 — Limites do nome
  When a Ana cria um projecto com um nome de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | n   | resultado                                                     |
    | 1   | o projecto é criado                                           |
    | 100 | o projecto é criado                                           |
    | 101 | a mensagem "Project name must be at most 100 characters."     |

Scenario Outline: CA05 — Limites da descrição
  When a Ana cria um projecto com uma descrição de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | n    | resultado                                                    |
    | 2000 | o projecto é criado                                          |
    | 2001 | a mensagem "Description must be at most 2000 characters."    |

Scenario: CA06 — Nome repetido é permitido
  Given já existe um projecto chamado "Website"
  When a Ana cria outro projecto com o nome "Website"
  Then é criado um segundo projecto, distinto do primeiro
  And os dois projectos têm tarefas e membros independentes
```
