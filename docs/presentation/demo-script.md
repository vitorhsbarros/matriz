# Matriz — Guião da demonstração

Para a apresentação final (5 minutos). A demonstração ocupa **2 minutos**, do diapositivo 3 ([index.html](index.html)), em **https://matriz-2s4n.onrender.com**.

As contas demo são pessoas fictícias (README.md, "Demo accounts"). As palavras-passe são as que definiste no Render, e **não estão escritas aqui**.

| Conta | Nome | *Website* | *App* |
|---|---|---|---|
| `ana@example.com` | Ana Silva | gestora | membro |
| `rui@example.com` | Rui Costa | membro | gestor |
| `sara@example.com` | Sara Lopes | não é membro | não é membro |

## Preparar antes da sessão

**Na véspera:**
- [ ] Entrar como **Ana**, **Rui** e **Sara**, para confirmar as três palavras-passe.
- [ ] Como **Ana**, em *Teams*, criar a equipa **Design** e adicionar o **Rui**. As equipas não vêm nos dados demo, e só se pode atribuir a uma tarefa uma equipa cujos membros estejam todos no projeto. A Ana e o Rui estão em *Website*.
- [ ] Opcional: criar duas ou três tarefas em *Website*, em colunas diferentes, para o quadro não aparecer vazio.

**15 minutos antes de apresentar:**
- [ ] Abrir https://matriz-2s4n.onrender.com. Se o serviço estiver a dormir (após 15 minutos sem tráfego), a primeira visita demora cerca de um minuto.
- [ ] Iniciar sessão como **Ana**, num separador.
- [ ] Abrir a apresentação, `docs/presentation/index.html`, noutro separador ou ecrã. Usar `P` para abrir as notas numa janela à parte, ou `N` para as ver por baixo dos diapositivos.
- [ ] Usar a aplicação de vez em quando até chegar a tua vez, para não voltar a adormecer.

**Plano B,** se a aplicação online não responder: correr localmente com `docker compose --profile app up -d --wait`, e abrir `http://localhost:8080` (README.md, "Running with Docker").

## Fluxo 1: o trabalho de uma equipa (cerca de 1 min 20 s)

Com sessão iniciada como **Ana**.

| # | Fazer | Dizer |
|---|---|---|
| 1 | Mostrar o **dashboard** (*My projects*) | "A página inicial: os meus projetos, o meu papel em cada um, e quantas tarefas há em cada estado." |
| 2 | Abrir **Website** | "Cada projeto tem um quadro: *To do*, *In progress* e *Done*." |
| 3 | **New task**, escrever o título "Preparar apresentação", prioridade **High**, depois **Create task** | "Criar uma tarefa: título obrigatório, prioridade, e data e descrição opcionais." |
| 4 | Arrastar o cartão para **In progress** | "Mudo o estado arrastando o cartão, e as contagens atualizam-se logo." |
| 5 | Abrir a tarefa e usar **Assign**: atribuir ao **Rui** e à equipa **Design** | "Uma tarefa pode ter várias pessoas e equipas, mas só do próprio projeto." |
| 6 | Escrever um comentário e carregar em **Comment** | "Os comentários ficam na tarefa. Só o autor os pode editar ou apagar." |

## Fluxo 2: as regras de acesso (cerca de 40 s)

| # | Fazer | Dizer |
|---|---|---|
| 1 | *Log out*, e depois entrar como **Rui** | "O Rui é membro de *Website*, mas não é o gestor." |
| 2 | Abrir **Website** | "Vê e muda as tarefas, mas não tem *Edit* nem *Delete* no projeto." |
| 3 | *Log out*, e depois entrar como **Sara** | "A Sara não é membro de nenhum projeto." |
| 4 | Mostrar o dashboard vazio. Opcionalmente, colar o endereço de *Website* | "Para ela, o projeto não existe: *Project not found.*" |

Voltar à apresentação (diapositivo 4).

## Se algo correr mal

- **A página demora:** o serviço está a acordar. Avançar para os diapositivos seguintes e voltar à demo no fim.
- **Uma palavra-passe não entra:** usar outra conta para mostrar o fluxo. As três estão na tabela acima.
- **A aplicação não responde de todo:** seguir o plano B, ou mostrar o fluxo nos diapositivos e na verificação registada (docs/verification.md §21).
