# US02 — Iniciar e terminar sessão

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 2 | RF02, RNF02 | US01 |

## User story

**Como** utilizador registado,
**quero** iniciar e terminar sessão com o meu email e palavra-passe,
**para** aceder aos meus projectos sem que outras pessoas o possam fazer em meu nome.

## Critérios de aceitação

O contexto comum aos cenários é que existe uma conta com o email "ana@example.com" e a palavra-passe "segura123".

### Cenário principal

```gherkin
Scenario: CA01 — Iniciar sessão com sucesso
  Given o visitante não tem sessão iniciada
  When inicia sessão com o email "ana@example.com" e a palavra-passe "segura123"
  Then a sessão fica iniciada com a conta "ana@example.com"
  And é apresentada a lista de projectos (US04)

Scenario: CA02 — Terminar sessão
  Given a Ana tem sessão iniciada
  When termina a sessão
  Then a sessão deixa de estar iniciada
  And ao tentar aceder a um projecto é apresentada a mensagem "Please log in to continue."
```

### Situações excepcionais

```gherkin
Scenario: CA03 — Palavra-passe errada
  When o visitante inicia sessão com o email "ana@example.com" e a palavra-passe "errada123"
  Then é apresentada a mensagem "Invalid email or password."
  And a sessão não é iniciada

Scenario: CA04 — Email não registado
  When o visitante inicia sessão com o email "rui@example.com" e qualquer palavra-passe
  Then é apresentada a mensagem "Invalid email or password."
  And a sessão não é iniciada

Scenario Outline: CA05 — Campos em falta
  When o visitante inicia sessão com o email <email> e a palavra-passe <password>
  Then é apresentada a mensagem "Email and password are required."
  And a sessão não é iniciada

  Examples:
    | email             | password    |
    | ""                | "segura123" |
    | "ana@example.com" | ""          |
    | ""                | ""          |

Scenario: CA06 — Visitante tenta aceder a uma funcionalidade protegida (RNF02)
  Given o visitante não tem sessão iniciada
  When tenta aceder à lista de projectos, a um projecto, a uma tarefa ou a uma equipa
  Then é apresentada a mensagem "Please log in to continue."
  And é-lhe pedido que inicie sessão
```

### Casos-limite

```gherkin
Scenario: CA07 — Email com maiúsculas diferentes
  When o visitante inicia sessão com o email "ANA@example.com" e a palavra-passe "segura123"
  Then a sessão fica iniciada com a conta "ana@example.com"

Scenario: CA08 — A palavra-passe distingue maiúsculas de minúsculas
  When o visitante inicia sessão com o email "ana@example.com" e a palavra-passe "SEGURA123"
  Then é apresentada a mensagem "Invalid email or password."

Scenario: CA09 — Mensagem não revela se o email existe
  Then a mensagem apresentada em CA03 e em CA04 é exactamente a mesma
```
