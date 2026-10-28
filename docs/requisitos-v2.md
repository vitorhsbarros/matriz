# Matriz — Requisitos (v2)

A Matriz é uma aplicação de gestão de projectos e tarefas, semelhante ao Jira mas muito simplificada. Permite a várias pessoas criar e gerir projectos e tarefas, actualizar o estado das tarefas e alocá-las a pessoas ou equipas.

**Fora de âmbito:** sprints, relatórios e integrações com sistemas externos.

Este documento refina [requisitos.md](requisitos.md) (v1). As alterações estão resumidas na secção 8.

## 1. Prioridades

As prioridades seguem o método **MoSCoW**:

| Prioridade | Significado | Onde entra |
|---|---|---|
| **Must** | Sem isto a aplicação não cumpre o seu propósito. | MVP |
| **Should** | Importante, mas a aplicação funciona sem isto. | Primeira versão a seguir ao MVP |
| **Could** | Útil, entra se houver tempo. | Funcionalidade futura |
| **Won't** | Não entra nesta fase. Fica registado para não se perder. | Funcionalidade futura |

**Funcionalidades essenciais** são as Must e as Should, ou seja, tudo o que está nas secções 3 e 4. **Funcionalidades futuras** são as Could e as Won't (secção 6).

## 2. Actores

| Actor | Responsabilidades |
|---|---|
| **Visitante** | Pessoa ainda sem sessão iniciada. Pode registar-se e iniciar sessão. |
| **Utilizador** | Pessoa com sessão iniciada. Cria projectos e equipas, e consulta os projectos de que é membro e as tarefas que lhe estão alocadas. |
| **Gestor de projecto** | Utilizador que criou um projecto. Gere os dados do projecto e os seus membros, e pode apagá-lo. |
| **Membro de projecto** | Utilizador adicionado a um projecto. Cria, edita, aloca e actualiza o estado das tarefas desse projecto. |
| **Gestor de equipa** | Utilizador que criou uma equipa. Gere o nome e os membros dessa equipa. |

O Gestor de projecto é também Membro desse projecto, e todos os papéis excepto o Visitante são Utilizadores.

## 3. Requisitos funcionais

Os IDs da v1 mantêm-se, para se poder comparar as duas versões. Os requisitos novos (RF17 e RF18) resultam de separar partes da v1 que tinham prioridades diferentes.

### Contas

| ID | Requisito | Actor(es) | Prioridade | Justificação |
|---|---|---|---|---|
| RF01 | O sistema deve permitir que um visitante crie uma conta com nome, email e palavra-passe. O email tem de ser único. | Visitante | Must | Sem contas não é possível identificar quem trabalha em cada projecto nem a quem alocar tarefas. |
| RF02 | O sistema deve permitir iniciar e terminar sessão com email e palavra-passe. | Visitante, Utilizador | Must | É a base do controlo de acesso (RNF02, RNF03). |

### Projectos

| ID | Requisito | Actor(es) | Prioridade | Justificação |
|---|---|---|---|---|
| RF03 | O sistema deve permitir criar um projecto com nome (obrigatório) e descrição (opcional). Quem cria o projecto fica como seu gestor. | Utilizador | Must | As tarefas pertencem sempre a um projecto, por isso sem projectos não há tarefas. |
| RF04 | O sistema deve permitir editar o nome e a descrição de um projecto. | Gestor de projecto | Should | Corrige erros e mantém a informação actualizada, mas um projecto funciona sem ser editado. |
| RF05 | O sistema deve permitir apagar um projecto, o que apaga também todas as suas tarefas. | Gestor de projecto | Should | Evita acumular projectos abandonados, mas não é necessário para validar a ideia. |
| RF06 | O sistema deve permitir adicionar e remover membros de um projecto, um a um. | Gestor de projecto | Must | Sem membros, só o gestor trabalha no projecto, o que contraria a ideia de várias pessoas colaborarem. |
| RF07 | O sistema deve listar os projectos de que o utilizador é membro. | Utilizador | Must | É o ponto de entrada do utilizador para chegar às tarefas. |
| RF18 | O sistema deve permitir adicionar de uma vez todos os membros de uma equipa a um projecto. | Gestor de projecto | Should | Poupa trabalho quando uma equipa inteira entra num projecto, mas o mesmo resultado consegue-se com RF06. |

### Equipas

| ID | Requisito | Actor(es) | Prioridade | Justificação |
|---|---|---|---|---|
| RF08 | O sistema deve permitir criar uma equipa com um nome. Quem cria a equipa fica como seu gestor. | Utilizador | Must | Alocar tarefas a equipas faz parte da ideia original da aplicação. |
| RF09 | O sistema deve permitir adicionar e remover utilizadores de uma equipa. | Gestor de equipa | Must | Uma equipa sem membros não tem utilidade. |

### Tarefas

| ID | Requisito | Actor(es) | Prioridade | Justificação |
|---|---|---|---|---|
| RF10 | O sistema deve permitir criar uma tarefa num projecto com título (obrigatório), descrição (opcional) e data limite (opcional). A tarefa começa no estado "To do". | Membro de projecto | Must | É a funcionalidade central da aplicação. |
| RF11 | O sistema deve permitir editar o título, a descrição e a data limite de uma tarefa. | Membro de projecto | Must | As tarefas mudam com frequência. Sem edição, a alternativa seria apagar e recriar, perdendo o estado e a alocação. |
| RF12 | O sistema deve permitir apagar uma tarefa. | Membro de projecto | Should | Remove tarefas criadas por engano, mas, no MVP, uma tarefa que já não interessa pode ser marcada como concluída. |
| RF13 | O sistema deve permitir alterar o estado de uma tarefa para "To do", "In progress" ou "Done". | Membro de projecto | Must | Acompanhar o progresso das tarefas faz parte da ideia original da aplicação. |
| RF14 | O sistema deve permitir alocar uma tarefa a um membro do projecto ou a uma equipa, e desfazer essa alocação. | Membro de projecto | Must | Saber quem é responsável por cada tarefa faz parte da ideia original da aplicação. |
| RF15 | O sistema deve listar as tarefas de um projecto, mostrando o estado e o responsável de cada uma. | Membro de projecto | Must | Sem esta listagem não há forma de ver o trabalho do projecto. |
| RF16 | O sistema deve listar as tarefas alocadas ao utilizador, incluindo as alocadas a equipas de que faz parte. | Utilizador | Should | Ajuda cada pessoa a saber o que tem para fazer sem percorrer todos os projectos, mas a informação já está disponível em RF15. |
| RF17 | O sistema deve permitir filtrar as tarefas de um projecto por estado e por responsável. | Membro de projecto | Should | Torna-se útil quando o projecto tem muitas tarefas, mas num projecto pequeno a listagem simples (RF15) chega. |

## 4. Requisitos não funcionais

| ID | Requisito | Prioridade | Justificação |
|---|---|---|---|
| RNF01 | Toda a interface e todas as mensagens do sistema devem estar em inglês. | Must | É a língua escolhida para a aplicação. |
| RNF02 | Só utilizadores com sessão iniciada podem aceder a projectos, equipas e tarefas. | Must | Os dados dos projectos não devem ser públicos. |
| RNF03 | Um utilizador só pode ver e alterar projectos e tarefas de que é membro. As acções reservadas a gestores (RF04, RF05, RF06, RF09, RF18) são recusadas a quem não tem esse papel. | Must | Sem esta regra, qualquer utilizador poderia alterar ou apagar o trabalho de outros. |
| RNF04 | As palavras-passe nunca podem ser guardadas nem mostradas em texto legível. | Must | Protege as contas dos utilizadores em caso de fuga de dados. |
| RNF05 | Apagar um projecto ou uma tarefa exige confirmação explícita do utilizador antes de ser executado. | Should | Evita perder dados por engano. Só se aplica quando RF05 e RF12 forem implementados. |
| RNF06 | A listagem de tarefas de um projecto com até 500 tarefas deve ser apresentada em menos de 2 segundos. | Should | Uma listagem lenta torna a aplicação incómoda de usar, mas no MVP os projectos terão poucas tarefas. |
| RNF07 | Uma alteração feita por um utilizador (ex.: mudança de estado) deve ficar visível para os restantes membros do projecto da próxima vez que estes consultarem o projecto ou a tarefa. | Must | Sem isto, vários membros não conseguem colaborar no mesmo projecto. |

## 5. Âmbito do MVP

O MVP é a versão mais pequena da Matriz que já permite a uma equipa organizar o trabalho num projecto. Inclui **todos os requisitos Must**:

- **Contas:** RF01, RF02
- **Projectos:** RF03, RF06, RF07
- **Equipas:** RF08, RF09
- **Tarefas:** RF10, RF11, RF13, RF14, RF15
- **Requisitos não funcionais:** RNF01, RNF02, RNF03, RNF04, RNF07

Com o MVP, um utilizador consegue criar conta, criar um projecto, juntar-lhe membros, criar tarefas, alocá-las a pessoas ou equipas e acompanhar o seu estado.

**Fica para depois do MVP** (Should): editar e apagar projectos (RF04, RF05), adicionar uma equipa inteira a um projecto (RF18), apagar tarefas (RF12), ver as minhas tarefas (RF16), filtrar tarefas (RF17), confirmação ao apagar (RNF05) e tempo de resposta (RNF06).

## 6. Funcionalidades futuras

Funcionalidades que não entram nas versões planeadas, mas que podem fazer sentido mais tarde.

| ID | Funcionalidade | Prioridade | Justificação |
|---|---|---|---|
| F01 | Notificar o utilizador quando lhe alocam uma tarefa. | Could | Evita que alguém não saiba que tem trabalho novo, mas RF16 já permite consultar as tarefas atribuídas. |
| F02 | Comentar tarefas. | Could | Permite discutir uma tarefa dentro da aplicação, mas a descrição da tarefa cobre o essencial. |
| F03 | Pesquisar tarefas por texto no título ou na descrição. | Could | Útil em projectos grandes, o que não é o caso esperado nesta fase. |
| F04 | Transferir o papel de gestor de um projecto ou equipa para outro utilizador. | Could | Resolve o caso de o gestor sair, mas é pouco frequente num projecto pequeno. |
| F05 | Ver o histórico de alterações de uma tarefa. | Won't | Ajuda a perceber quem mudou o quê, mas acrescenta complexidade sem ser necessário para gerir tarefas. |
| F06 | Configurar os estados das tarefas por projecto. | Won't | Os três estados fixos chegam para um projecto simples, e torná-los configuráveis aproxima a aplicação da complexidade do Jira. |
| F07 | Criar subtarefas ou dependências entre tarefas. | Won't | Afasta-se do objectivo de ser uma aplicação muito simplificada. |

Sprints, relatórios e integrações com sistemas externos continuam fora de âmbito e não estão listados aqui.

## 7. Pressupostos

1. **Registo aberto:** qualquer pessoa pode criar conta. Não existe um papel de administrador do sistema.
2. **Um gestor por projecto e por equipa:** o gestor é quem cria o projecto ou a equipa, e esse papel não se transfere (ver F04).
3. **Estados fixos:** uma tarefa tem sempre um de três estados ("To do", "In progress", "Done"), que não são configuráveis (ver F06). Qualquer transição entre eles é permitida.
4. **Um responsável por tarefa:** uma tarefa está alocada, no máximo, a uma pessoa *ou* a uma equipa, nunca às duas. Pode também não estar alocada.
5. **Equipas independentes dos projectos:** uma equipa existe fora dos projectos e pode trabalhar em vários. Só se pode alocar uma tarefa a uma equipa cujos membros pertençam todos ao projecto.
6. **Remoção de membros:** quando alguém sai de um projecto, as tarefas alocadas a essa pessoa nesse projecto ficam sem responsável.
7. **Língua:** a aplicação está em inglês (RNF01), mas a documentação do projecto continua em português.

## 8. Alterações em relação à v1

| Alteração | Detalhe |
|---|---|
| RNF01 alterado | A aplicação passa de português de Portugal para inglês. Os nomes dos estados passam a "To do", "In progress" e "Done" (RF10, RF13). |
| Prioridades e justificações | Cada requisito tem agora uma prioridade MoSCoW e uma justificação. |
| Âmbito do MVP | Nova secção 5, com os requisitos Must. |
| Funcionalidades futuras | Nova secção 6 (F01–F07). Os pressupostos da v1 sobre notificações, comentários e subtarefas passaram a funcionalidades futuras. |
| RF06 dividido | Adicionar uma equipa inteira a um projecto passou a RF18 (Should). RF06 fica só com membros individuais (Must). |
| RF15 dividido | A filtragem passou a RF17 (Should). RF15 fica só com a listagem (Must). |
