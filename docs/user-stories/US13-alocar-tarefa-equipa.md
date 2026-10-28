# US13 — Alocar tarefa a uma equipa

| Prioridade | Ordem | Requisitos | Depende de |
|---|---|---|---|
| Must (MVP) | 13 | RF14 | US10, US12 |

## User story

**Como** membro de projecto,
**quero** alocar uma tarefa a uma equipa,
**para** que um grupo de pessoas fique responsável por ela em conjunto.

## Regras

- Uma tarefa tem, no máximo, um responsável: uma pessoa ([US10](US10-alocar-tarefa-pessoa.md)) ou uma equipa.
- Só se pode alocar a uma equipa cujos membros sejam todos membros do projecto. Esta regra só é verificada no momento da alocação (D09).
- Alocar substitui o responsável anterior, seja uma pessoa ou outra equipa.
- A alocação a uma equipa desfaz-se da mesma forma que a alocação a uma pessoa (US10, CA03).

## Critérios de aceitação

O contexto comum aos cenários é que a Ana, o Rui e a Sara são membros do projecto "Website", e a equipa "Design" tem como membros o Rui e a Sara.

### Cenário principal

```gherkin
Scenario: CA01 — Alocar a uma equipa
  Given a tarefa "Criar ícones" não tem responsável
  When a Ana aloca a tarefa "Criar ícones" à equipa "Design"
  Then o responsável da tarefa passa a ser a equipa "Design"
  And a lista de tarefas mostra "Design (team)" como responsável

Scenario: CA02 — Substituir uma pessoa por uma equipa
  Given a tarefa "Criar ícones" está alocada ao Rui
  When a Ana aloca a tarefa à equipa "Design"
  Then o responsável da tarefa passa a ser a equipa "Design"
  And o Rui deixa de ser responsável individual pela tarefa

Scenario: CA03 — Substituir uma equipa por uma pessoa
  Given a tarefa "Criar ícones" está alocada à equipa "Design"
  When a Ana aloca a tarefa à Sara
  Then o responsável da tarefa passa a ser a Sara
  And a equipa "Design" deixa de ser responsável pela tarefa
```

### Situações excepcionais

```gherkin
Scenario: CA04 — Equipa com membros fora do projecto
  Given a equipa "Marketing" tem como membros o Rui e a Marta
  And a Marta não é membro do projecto "Website"
  When a Ana tenta alocar a tarefa "Criar ícones" à equipa "Marketing"
  Then é apresentada a mensagem "All team members must belong to the project."
  And o responsável da tarefa não muda

Scenario: CA05 — Equipa inexistente
  When a Ana tenta alocar a tarefa "Criar ícones" a uma equipa que não existe
  Then é apresentada a mensagem "Team not found."
  And o responsável da tarefa não muda
```

### Casos-limite

```gherkin
Scenario: CA06 — Equipa sem outros membros além do gestor
  Given a equipa "QA" tem apenas a Ana como membro
  When a Ana aloca a tarefa "Criar ícones" à equipa "QA"
  Then o responsável da tarefa passa a ser a equipa "QA"

Scenario: CA07 — Membros da equipa mudam depois da alocação (D09)
  Given a tarefa "Criar ícones" está alocada à equipa "Design"
  When o gestor da equipa "Design" adiciona a Marta, que não é membro do projecto "Website"
  Then a tarefa "Criar ícones" continua alocada à equipa "Design"

Scenario: CA08 — Membro da equipa removido do projecto (D10)
  Given a tarefa "Criar ícones" está alocada à equipa "Design"
  When o gestor do projecto remove o Rui do projecto "Website"
  Then a tarefa "Criar ícones" continua alocada à equipa "Design"
```
