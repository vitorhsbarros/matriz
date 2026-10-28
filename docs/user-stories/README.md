# Matriz — Backlog inicial (MVP)

Este backlog converte os requisitos Must de [requisitos-v2.md](../requisitos-v2.md) em user stories (US) verificáveis. Cada US tem o seu ficheiro nesta pasta.

## 1. Backlog

Todas as US do MVP têm prioridade **Must**. A coluna **Ordem** indica a sequência de implementação recomendada, definida pelas dependências entre US e pelo valor que cada uma entrega.

| Ordem | US | Título | Requisitos | Depende de |
|---|---|---|---|---|
| 1 | [US01](US01-registar-conta.md) | Registar conta | RF01, RNF04 | — |
| 2 | [US02](US02-iniciar-terminar-sessao.md) | Iniciar e terminar sessão | RF02, RNF02 | US01 |
| 3 | [US03](US03-criar-projecto.md) | Criar projecto | RF03 | US02 |
| 4 | [US04](US04-ver-os-meus-projectos.md) | Ver os meus projectos | RF07, RNF03 | US03 |
| 5 | [US05](US05-criar-tarefa.md) | Criar tarefa | RF10 | US03 |
| 6 | [US06](US06-listar-tarefas-projecto.md) | Listar as tarefas de um projecto | RF15 | US05 |
| 7 | [US07](US07-alterar-estado-tarefa.md) | Alterar o estado de uma tarefa | RF13, RNF07 | US05 |
| 8 | [US08](US08-editar-tarefa.md) | Editar tarefa | RF11 | US05 |
| 9 | [US09](US09-gerir-membros-projecto.md) | Gerir os membros de um projecto | RF06, RNF03 | US03 |
| 10 | [US10](US10-alocar-tarefa-pessoa.md) | Alocar tarefa a uma pessoa | RF14 | US05, US09 |
| 11 | [US11](US11-criar-equipa.md) | Criar equipa | RF08 | US02 |
| 12 | [US12](US12-gerir-membros-equipa.md) | Gerir os membros de uma equipa | RF09 | US11 |
| 13 | [US13](US13-alocar-tarefa-equipa.md) | Alocar tarefa a uma equipa | RF14 | US10, US12 |

O RF14 foi dividido em duas US (US10 e US13). A alocação a pessoas pode ser entregue antes das equipas existirem, o que torna o produto utilizável mais cedo.

## 2. Critérios transversais (Definition of Done)

Estes critérios aplicam-se a **todas** as US e não são repetidos em cada ficheiro. Uma US só está concluída quando os seus critérios de aceitação e os critérios abaixo são cumpridos.

### DoD01 — Interface em inglês (RNF01)

```gherkin
Scenario: Todo o texto apresentado está em inglês
  Given qualquer ecrã ou mensagem da funcionalidade
  When o utilizador o vê
  Then todos os textos, botões, rótulos e mensagens estão em inglês
```

### DoD02 — Acesso apenas com sessão iniciada (RNF02)

```gherkin
Scenario: Visitante tenta aceder a uma funcionalidade protegida
  Given um visitante sem sessão iniciada
  When tenta aceder a qualquer projecto, equipa ou tarefa
  Then o acesso é recusado
  And é apresentada a mensagem "Please log in to continue."
  And é-lhe pedido que inicie sessão
```

### DoD03 — Acesso apenas a projectos de que é membro (RNF03)

```gherkin
Scenario: Utilizador tenta aceder a um projecto de que não é membro
  Given o utilizador "Ana" não é membro do projecto "Website"
  When tenta ver ou alterar o projecto "Website" ou qualquer uma das suas tarefas
  Then o acesso é recusado
  And é apresentada a mensagem "Project not found."
  And nenhum dado do projecto é mostrado ou alterado
```

A mensagem é a mesma de um projecto inexistente. Assim, um utilizador não consegue descobrir que projectos existem.

### DoD04 — Mensagens exactas

As mensagens entre aspas nos critérios de aceitação são o texto exacto a apresentar. Quando uma acção é recusada por uma validação, nenhum dado é criado nem alterado.

## 3. Decisões tomadas para eliminar ambiguidades

Os requisitos deixavam alguns pontos em aberto. Estas decisões aplicam-se a todas as US.

| # | Decisão |
|---|---|
| D01 | **Espaços nas pontas:** os campos de texto são guardados sem espaços no início e no fim. Um campo obrigatório só com espaços conta como vazio. |
| D02 | **Limites de tamanho:** os limites contam caracteres depois de aplicar D01. |
| D03 | **Emails:** comparam-se sem distinguir maiúsculas de minúsculas ("Ana@Example.com" e "ana@example.com" são o mesmo email). |
| D04 | **Adicionar pessoas:** pessoas são adicionadas a projectos e equipas através do email da sua conta. |
| D05 | **Equipas visíveis a todos:** qualquer utilizador vê a lista de equipas, com o nome e os membros de cada uma, para poder alocar-lhes tarefas. |
| D06 | **Nomes:** os nomes de projectos podem repetir-se, mas os nomes de equipas são únicos, porque as equipas são escolhidas pelo nome ao alocar tarefas. |
| D07 | **Gestores também são membros:** quem cria um projecto ou uma equipa fica como gestor e como membro. O gestor não pode ser removido. |
| D08 | **Qualquer membro altera tarefas:** qualquer membro do projecto pode editar, mudar o estado e alocar qualquer tarefa desse projecto, não só o responsável. |
| D09 | **Alocação a equipas só é verificada no momento da alocação:** a regra "todos os membros da equipa pertencem ao projecto" só é verificada quando se aloca a tarefa. Alterações posteriores à equipa não desfazem alocações existentes. |
| D10 | **Remover um membro do projecto:** as tarefas alocadas a essa pessoa nesse projecto ficam sem responsável. As tarefas alocadas a equipas não são afectadas. |
| D11 | **Alterações em simultâneo:** se dois membros alterarem a mesma tarefa ao mesmo tempo, prevalece a última alteração guardada. |
| D12 | **Datas limite:** uma data limite nova ou alterada não pode ser anterior ao dia actual. Uma data limite já passada que não é alterada continua válida. |

## 4. Rastreabilidade

Todos os requisitos do MVP estão cobertos por pelo menos uma US ou por um critério transversal.

| Requisito | Coberto por |
|---|---|
| RF01 | US01 |
| RF02 | US02 |
| RF03 | US03 |
| RF06 | US09 |
| RF07 | US04 |
| RF08 | US11 |
| RF09 | US12 |
| RF10 | US05 |
| RF11 | US08 |
| RF13 | US07 |
| RF14 | US10, US13 |
| RF15 | US06 |
| RNF01 | DoD01 |
| RNF02 | US02, DoD02 |
| RNF03 | US04, US09, DoD03 |
| RNF04 | US01 |
| RNF07 | US07 |

Os requisitos Should (RF04, RF05, RF12, RF16, RF17, RF18, RNF05, RNF06) ficam fora deste backlog e serão convertidos em US depois do MVP.
