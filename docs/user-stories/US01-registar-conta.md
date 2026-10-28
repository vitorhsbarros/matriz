# US01 — Registar conta

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 1 | RF01, RNF04 | — |

## User story

**Como** visitante,
**quero** criar uma conta com o meu nome, email e palavra-passe,
**para** poder participar em projectos da Matriz.

## Regras

| Campo | Regras |
|---|---|
| Nome | Obrigatório, entre 1 e 100 caracteres. |
| Email | Obrigatório, no formato `texto@domínio.extensão`, com no máximo 254 caracteres. Tem de ser único (sem distinguir maiúsculas de minúsculas, D03). |
| Palavra-passe | Obrigatória, entre 8 e 128 caracteres. Não lhe são retirados espaços. |

## Critérios de aceitação

### Cenário principal

```gherkin
Scenario: CA01 — Registo com sucesso
  Given não existe nenhuma conta com o email "ana@example.com"
  When o visitante se regista com o nome "Ana Silva", o email "ana@example.com" e a palavra-passe "segura123"
  Then é criada uma conta com o nome "Ana Silva" e o email "ana@example.com"
  And a sessão fica iniciada com essa conta
  And é apresentada a lista de projectos (US04)
```

### Situações excepcionais

```gherkin
Scenario: CA02 — Email já registado
  Given já existe uma conta com o email "ana@example.com"
  When o visitante se regista com o email "ana@example.com"
  Then é apresentada a mensagem "This email is already in use."
  And não é criada nenhuma conta

Scenario: CA03 — Email já registado com maiúsculas diferentes
  Given já existe uma conta com o email "ana@example.com"
  When o visitante se regista com o email "Ana@Example.COM"
  Then é apresentada a mensagem "This email is already in use."
  And não é criada nenhuma conta

Scenario Outline: CA04 — Campos obrigatórios em falta
  When o visitante se regista deixando o campo <campo> com o valor <valor>
  Then é apresentada a mensagem <mensagem>
  And não é criada nenhuma conta

  Examples:
    | campo         | valor | mensagem                  |
    | nome          | ""    | "Name is required."       |
    | nome          | "   " | "Name is required."       |
    | email         | ""    | "Email is required."      |
    | palavra-passe | ""    | "Password is required."   |

Scenario Outline: CA05 — Email com formato inválido
  When o visitante se regista com o email <email>
  Then é apresentada a mensagem "Enter a valid email address."
  And não é criada nenhuma conta

  Examples:
    | email              |
    | "ana"              |
    | "ana@"             |
    | "ana.example.com"  |
    | "@example.com"     |
    | "ana@example"      |
```

### Casos-limite

```gherkin
Scenario Outline: CA06 — Limites do nome
  When o visitante se regista com um nome de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | n   | resultado                                                  |
    | 1   | a conta é criada                                           |
    | 100 | a conta é criada                                           |
    | 101 | a mensagem "Name must be at most 100 characters."          |

Scenario Outline: CA07 — Limites da palavra-passe
  When o visitante se regista com uma palavra-passe de <n> caracteres
  Then o resultado é <resultado>

  Examples:
    | n   | resultado                                                  |
    | 7   | a mensagem "Password must be at least 8 characters."       |
    | 8   | a conta é criada                                           |
    | 128 | a conta é criada                                           |
    | 129 | a mensagem "Password must be at most 128 characters."      |

Scenario: CA08 — Espaços nas pontas do nome e do email
  When o visitante se regista com o nome "  Ana Silva  " e o email "  ana@example.com  "
  Then a conta é criada com o nome "Ana Silva" e o email "ana@example.com"
```

### Segurança da palavra-passe (RNF04)

```gherkin
Scenario: CA09 — Palavra-passe escondida ao escrever
  When o visitante escreve a palavra-passe no formulário de registo
  Then cada carácter é apresentado como um símbolo de máscara e não como o carácter escrito

Scenario: CA10 — Palavra-passe não guardada em texto legível
  Given o visitante se registou com a palavra-passe "segura123"
  When os dados guardados dessa conta são inspeccionados
  Then o texto "segura123" não aparece em nenhum dos dados guardados
```
