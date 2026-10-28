# US10 — Alocar tarefa a uma pessoa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 10 | RF14 | US05, US09 |

## User story

**Como** membro de projecto,
**quero** alocar uma tarefa a um membro do projecto, ou desfazer essa alocação,
**para** que fique claro quem é responsável por ela.

## Regras

- Uma tarefa tem, no máximo, um responsável: uma pessoa ou uma equipa ([US13](US13-alocar-tarefa-equipa.md)).
- Só se pode alocar a uma pessoa que seja membro do projecto.
- Alocar uma tarefa que já tem responsável substitui o responsável anterior.
- Qualquer membro do projecto pode alocar qualquer tarefa do projecto (D08).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana e o Rui são membros do projecto "Website", que tem a tarefa "Testar o site" sem responsável.

### Cenário principal

```gherkin
Scenario: CA01 — Alocar a um membro
  When a Ana aloca a tarefa "Testar o site" ao Rui
  Then o responsável da tarefa passa a ser o Rui
  And a lista de tarefas mostra "Rui Costa" como responsável

Scenario: CA02 — Substituir o responsável
  Given a tarefa "Testar o site" está alocada ao Rui
  When a Ana aloca a tarefa a si própria
  Then o responsável da tarefa passa a ser a Ana
  And o Rui deixa de ser responsável pela tarefa

Scenario: CA03 — Desfazer a alocação
  Given a tarefa "Testar o site" está alocada ao Rui
  When a Ana desfaz a alocação da tarefa
  Then a tarefa fica sem responsável ("Unassigned")
```

### Situações excepcionais

```gherkin
Scenario: CA04 — Pessoa que não é membro do projecto
  Given a Marta não é membro do projecto "Website"
  When a Ana tenta alocar a tarefa "Testar o site" à Marta
  Then é apresentada a mensagem "The assignee must be a member of the project."
  And a tarefa continua sem responsável

Scenario: CA05 — Membro removido entretanto
  Given a Ana abriu a tarefa "Testar o site" para a alocar ao Rui
  And entretanto o gestor removeu o Rui do projecto
  When a Ana confirma a alocação ao Rui
  Then é apresentada a mensagem "The assignee must be a member of the project."
  And a tarefa continua sem responsável

Scenario: CA06 — Utilizador que não é membro do projecto
  Given a Marta não é membro do projecto "Website"
  When a Marta tenta alocar a tarefa "Testar o site"
  Then é apresentada a mensagem "Project not found."
  And a tarefa continua sem responsável
```

### Casos-limite

```gherkin
Scenario: CA07 — Alocar ao responsável actual
  Given a tarefa "Testar o site" está alocada ao Rui
  When a Ana aloca a tarefa ao Rui
  Then o responsável continua a ser o Rui
  And não é apresentada nenhuma mensagem de erro

Scenario: CA08 — Desfazer a alocação de uma tarefa sem responsável
  Given a tarefa "Testar o site" não tem responsável
  When a Ana desfaz a alocação
  Then a tarefa continua sem responsável
  And não é apresentada nenhuma mensagem de erro

Scenario: CA09 — Alocar não altera outros campos
  When a Ana aloca a tarefa "Testar o site" ao Rui
  Then o título, a descrição, a data limite e o estado da tarefa não mudam
```
