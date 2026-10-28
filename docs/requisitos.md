# Matriz — Requisitos

A Matriz é uma aplicação de gestão de projectos e tarefas, semelhante ao Jira mas muito simplificada. Permite a várias pessoas criar e gerir projectos e tarefas, actualizar o estado das tarefas e alocá-las a pessoas ou equipas.

**Fora de âmbito:** sprints, relatórios e integrações com sistemas externos.

## 1. Actores

| Actor | Responsabilidades |
|---|---|
| **Visitante** | Pessoa ainda sem sessão iniciada. Pode registar-se e iniciar sessão. |
| **Utilizador** | Pessoa com sessão iniciada. Cria projectos e equipas, e consulta os projectos de que é membro e as tarefas que lhe estão alocadas. |
| **Gestor de projecto** | Utilizador que criou um projecto. Gere os dados do projecto e os seus membros, e pode apagá-lo. |
| **Membro de projecto** | Utilizador adicionado a um projecto. Cria, edita, aloca e actualiza o estado das tarefas desse projecto. |
| **Gestor de equipa** | Utilizador que criou uma equipa. Gere o nome e os membros dessa equipa. |

O Gestor de projecto é também Membro desse projecto, e todos os papéis excepto o Visitante são Utilizadores. Assim, o que um Membro de projecto pode fazer, o Gestor de projecto também pode.

## 2. Requisitos funcionais

### Contas

| ID | Requisito | Actor(es) |
|---|---|---|
| RF01 | O sistema deve permitir que um visitante crie uma conta com nome, email e palavra-passe. O email tem de ser único. | Visitante |
| RF02 | O sistema deve permitir iniciar e terminar sessão com email e palavra-passe. | Visitante, Utilizador |

### Projectos

| ID | Requisito | Actor(es) |
|---|---|---|
| RF03 | O sistema deve permitir criar um projecto com nome (obrigatório) e descrição (opcional). Quem cria o projecto fica como seu gestor. | Utilizador |
| RF04 | O sistema deve permitir editar o nome e a descrição de um projecto. | Gestor de projecto |
| RF05 | O sistema deve permitir apagar um projecto, o que apaga também todas as suas tarefas. | Gestor de projecto |
| RF06 | O sistema deve permitir adicionar e remover membros de um projecto, um a um ou adicionando todos os membros de uma equipa de uma vez. | Gestor de projecto |
| RF07 | O sistema deve listar os projectos de que o utilizador é membro. | Utilizador |

### Equipas

| ID | Requisito | Actor(es) |
|---|---|---|
| RF08 | O sistema deve permitir criar uma equipa com um nome. Quem cria a equipa fica como seu gestor. | Utilizador |
| RF09 | O sistema deve permitir adicionar e remover utilizadores de uma equipa. | Gestor de equipa |

### Tarefas

| ID | Requisito | Actor(es) |
|---|---|---|
| RF10 | O sistema deve permitir criar uma tarefa num projecto com título (obrigatório), descrição (opcional) e data limite (opcional). A tarefa começa no estado "A fazer". | Membro de projecto |
| RF11 | O sistema deve permitir editar o título, a descrição e a data limite de uma tarefa. | Membro de projecto |
| RF12 | O sistema deve permitir apagar uma tarefa. | Membro de projecto |
| RF13 | O sistema deve permitir alterar o estado de uma tarefa para "A fazer", "Em curso" ou "Concluída". | Membro de projecto |
| RF14 | O sistema deve permitir alocar uma tarefa a um membro do projecto ou a uma equipa, e desfazer essa alocação. | Membro de projecto |
| RF15 | O sistema deve listar as tarefas de um projecto e permitir filtrá-las por estado e por responsável. | Membro de projecto |
| RF16 | O sistema deve listar as tarefas alocadas ao utilizador, incluindo as alocadas a equipas de que faz parte. | Utilizador |

## 3. Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF01 | Toda a interface e todas as mensagens do sistema devem estar em português de Portugal. |
| RNF02 | Só utilizadores com sessão iniciada podem aceder a projectos, equipas e tarefas. |
| RNF03 | Um utilizador só pode ver e alterar projectos e tarefas de que é membro. As acções reservadas a gestores (RF04, RF05, RF06, RF09) são recusadas a quem não tem esse papel. |
| RNF04 | As palavras-passe nunca podem ser guardadas nem mostradas em texto legível. |
| RNF05 | Apagar um projecto ou uma tarefa exige confirmação explícita do utilizador antes de ser executado. |
| RNF06 | A listagem de tarefas de um projecto com até 500 tarefas deve ser apresentada em menos de 2 segundos. |
| RNF07 | Uma alteração feita por um utilizador (ex.: mudança de estado) deve ficar visível para os restantes membros do projecto da próxima vez que estes consultarem o projecto ou a tarefa. |

## 4. Pressupostos

1. **Registo aberto:** qualquer pessoa pode criar conta. Não existe um papel de administrador do sistema.
2. **Um gestor por projecto e por equipa:** o gestor é quem cria o projecto ou a equipa, e esse papel não se transfere.
3. **Estados fixos:** uma tarefa tem sempre um de três estados ("A fazer", "Em curso", "Concluída"), que não são configuráveis. Qualquer transição entre eles é permitida.
4. **Um responsável por tarefa:** uma tarefa está alocada, no máximo, a uma pessoa *ou* a uma equipa, nunca às duas. Pode também não estar alocada.
5. **Equipas independentes dos projectos:** uma equipa existe fora dos projectos e pode trabalhar em vários. Só se pode alocar uma tarefa a uma equipa cujos membros pertençam todos ao projecto. Para isso, o gestor adiciona a equipa ao projecto (RF06).
6. **Remoção de membros:** quando alguém sai de um projecto, as tarefas alocadas a essa pessoa nesse projecto ficam sem responsável.
7. **Sem hierarquia de tarefas:** não há subtarefas, dependências entre tarefas, comentários nem anexos.
8. **Sem notificações:** os utilizadores não recebem avisos, por exemplo quando lhes alocam uma tarefa. Consultam as suas tarefas através de RF16.
