# US07 — Alterar o estado de uma tarefa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 7 | RF13, RNF07 | US05 |

## User story

**Como** membro de projecto,
**quero** mudar o estado de uma tarefa,
**para** que todos saibam em que ponto está o trabalho.

## Regras

- Os estados possíveis são "To do", "In progress" e "Done".
- É permitida qualquer transição entre estados, incluindo voltar atrás.
- Qualquer membro do projecto pode mudar o estado de qualquer tarefa do projecto, mesmo que não seja o responsável (D08).
- Mudar o estado não altera nenhum outro campo da tarefa.

## Critérios de aceitação

O contexto comum aos cenários é que a Ana e o Rui são membros do projecto "Website", que tem a tarefa "Testar o site" no estado "To do".

### Cenário principal

```gherkin
Scenario: CA01 — Mudar o estado de uma tarefa
  When a Ana muda o estado da tarefa "Testar o site" para "In progress"
  Then o estado da tarefa passa a ser "In progress"
  And o título, a descrição, a data limite e o responsável não mudam

Scenario Outline: CA02 — Todas as transições são permitidas
  Given a tarefa "Testar o site" está no estado <de>
  When a Ana muda o estado para <para>
  Then o estado da tarefa passa a ser <para>

  Examples:
    | de          | para        |
    | To do       | In progress |
    | To do       | Done        |
    | In progress | To do       |
    | In progress | Done        |
    | Done        | To do       |
    | Done        | In progress |

Scenario: CA03 — A alteração fica visível para os outros membros (RNF07)
  Given a Ana mudou o estado da tarefa "Testar o site" para "Done"
  When o Rui abre o projecto "Website" depois dessa alteração
  Then o Rui vê a tarefa "Testar o site" com o estado "Done"
```

### Situações excepcionais

```gherkin
Scenario: CA04 — Estado inválido
  When é pedido que o estado da tarefa "Testar o site" passe a "Blocked"
  Then é apresentada a mensagem "Invalid status."
  And o estado da tarefa continua "To do"

Scenario: CA05 — Utilizador que não é membro do projecto
  Given a Marta não é membro do projecto "Website"
  When a Marta tenta mudar o estado da tarefa "Testar o site"
  Then é apresentada a mensagem "Project not found."
  And o estado da tarefa continua "To do"
```

### Casos-limite

```gherkin
Scenario: CA06 — Escolher o estado actual
  When a Ana muda o estado da tarefa "Testar o site" para "To do"
  Then o estado da tarefa continua "To do"
  And não é apresentada nenhuma mensagem de erro

Scenario: CA07 — Membro que não é o responsável
  Given a tarefa "Testar o site" está alocada ao Rui
  When a Ana muda o estado da tarefa para "Done"
  Then o estado da tarefa passa a ser "Done"

Scenario: CA08 — Duas alterações em simultâneo (D11)
  Given a Ana e o Rui têm a tarefa "Testar o site" aberta
  When a Ana muda o estado para "In progress"
  And a seguir o Rui muda o estado para "Done"
  Then o estado final da tarefa é "Done"
```
