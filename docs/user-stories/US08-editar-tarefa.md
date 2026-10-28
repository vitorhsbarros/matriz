# US08 — Editar tarefa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 8 | RF11 | US05 |

## User story

**Como** membro de projecto,
**quero** editar o título, a descrição e a data limite de uma tarefa,
**para** manter a informação da tarefa correcta sem ter de a recriar.

## Regras

- As validações dos campos são as mesmas de [US05](US05-criar-tarefa.md).
- Editar não altera o estado nem o responsável da tarefa.
- A descrição e a data limite podem ser apagadas.
- Qualquer membro do projecto pode editar qualquer tarefa do projecto (D08).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana é membro do projecto "Website", hoje é 2026-11-02 e o projecto tem a tarefa:

| título | descrição | data limite | estado | responsável |
|---|---|---|---|---|
| Testar o site | Testar em telemóvel | 2026-11-20 | In progress | Rui Costa |

### Cenário principal

```gherkin
Scenario: CA01 — Editar todos os campos
  When a Ana edita a tarefa "Testar o site" para:
    | título      | Testar o site em produção      |
    | descrição   | Testar em telemóvel e tablet   |
    | data limite | 2026-11-25                     |
  Then a tarefa fica com esses dados
  And o estado continua "In progress"
  And o responsável continua "Rui Costa"

Scenario: CA02 — Apagar a descrição e a data limite
  When a Ana apaga a descrição e a data limite da tarefa "Testar o site"
  Then a tarefa fica sem descrição e sem data limite
```

### Situações excepcionais

```gherkin
Scenario: CA03 — Título apagado
  When a Ana edita o título da tarefa "Testar o site" para ""
  Then é apresentada a mensagem "Task title is required."
  And a tarefa mantém todos os dados anteriores

Scenario: CA04 — Nova data limite no passado
  When a Ana muda a data limite da tarefa "Testar o site" para 2026-11-01
  Then é apresentada a mensagem "Due date cannot be in the past."
  And a data limite continua 2026-11-20

Scenario: CA05 — Erro num campo não guarda os outros
  When a Ana muda a descrição para "Nova descrição" e o título para ""
  Then é apresentada a mensagem "Task title is required."
  And a descrição continua "Testar em telemóvel"

Scenario: CA06 — Utilizador que não é membro do projecto
  Given a Marta não é membro do projecto "Website"
  When a Marta tenta editar a tarefa "Testar o site"
  Then é apresentada a mensagem "Project not found."
  And a tarefa mantém todos os dados anteriores
```

### Casos-limite

```gherkin
Scenario: CA07 — Data limite já passada que não é alterada (D12)
  Given a tarefa "Testar o site" tem a data limite 2026-10-30, que já passou
  When a Ana edita apenas o título da tarefa para "Testar o site final"
  Then a alteração é guardada
  And a data limite continua 2026-10-30

Scenario: CA08 — Guardar sem alterações
  When a Ana guarda a tarefa "Testar o site" sem mudar nenhum campo
  Then a tarefa mantém todos os dados
  And não é apresentada nenhuma mensagem de erro

Scenario: CA09 — Duas edições em simultâneo (D11)
  Given a Ana e o Rui têm a tarefa "Testar o site" aberta para edição
  When a Ana guarda o título "Título A"
  And a seguir o Rui guarda o título "Título B"
  Then o título final da tarefa é "Título B"
```
