<p align="center"><img src="docs/logo.png" width="320" alt="Drakebound"></p>

# Drakebound

*From the nest to the skies.* Mod de dragões para Minecraft **26.1.2** (NeoForge **26.1.2.125**, Java 25).

## Compilar

```bash
./gradlew build          # jar em build/libs/
./gradlew runClient      # abre o jogo com o mod
```

Precisa de um JDK 25.

## O que já existe

| Dragão | Onde aparece | Sopro (todos deitam fogo, mais este efeito) |
|---|---|---|
| Verdejante (Verdant) | Ninhos nas Cavernas Verdejantes | Veneno |
| Dos Picos (Peaks) | Ninhos no topo das montanhas (picos) | Elétrico (lentidão) |
| Áridas (Badlands) | Ninhos nas Badlands | Só fogo, que queima mais tempo |
| Saqueador (Raider) | 15% por vaga de raid e 10% nos postos avançados, montado por um Pillager | Escuridão |
| Do Ender (Ender Dragon) | Ninho único na ilha principal do End, fora dos pilares | Levitação (vida e dano x1,2) |
| Ancião do Nether (Nether Ancient) | Ninho antigo e raro no Nether (vida e dano x1,5) | Fogo de almas e definhamento |

- **Machos**: 240 de vida (120 corações) e cornos. **Fêmeas**: 200 de vida e mais dano.
- Todos são imunes a fogo, lava e magma.
- **Ninhos**: 20% estão abandonados, sem dragão nem minério, só com o esqueleto do antigo dono (blocos de osso de dragão e um crânio de dragão) e os baús. O bloco de osso de dragão faz-se com 9 ossos e partido devolve-os; o crânio pousa-se ou usa-se na cabeça. plataforma irregular de Terra Queimada com barro compactado, terra infértil, terra enraizada, magma e blocos de ouro, ferro e cobre brutos, pilares em ruína e 1 a 2 baús de tesouro. Os machos guardam mais minério e as fêmeas guardam 0 a 2 ovos (37% nenhum, 60% um, 3% dois) sobre o magma central,. Os dragões selvagens atacam quem se aproxima do ninho.
- **Terra Queimada**: forma-se quando um dragão de fogo sopra durante muito tempo sobre terra.
- **Ovo do Ender Dragon do jogo**: colocado em cima de magma e aceso com isqueiro, choca uma cria do Dragão do Ender domesticável.
- **Ovos**: só chocam em cima de um bloco de magma e depois de acesos com um isqueiro, em cerca de 10 minutos. Tirar o magma apaga o ovo (o progresso fica guardado). Ao nascer, o dono é o jogador mais próximo num raio de 16 blocos.
- **Domesticação**: só crias. Sem dono, domestica-se com carne crua, com 5% de rejeição por tentativa. Adultos selvagens não se domesticam.
- **Crescimento**: recém-nascido, bebé, criança, adolescente e adulto, cada fase com o seu tamanho (3 dias de jogo). A carne crua acelera um pouco; o Bloco de Carniça (9 carnes podres) avança um quarto do crescimento de uma vez.
- **Armaduras de dragão**: uma por cor, feitas e reparadas com as escamas dessa cor (mesma forma das armaduras do jogo). Protegem como netherite, não ardem na lava e aceitam moldes de embelezamento na mesa de ferraria.
- **Reprodução**: dois dragões domesticados, adultos, da mesma cor e de sexos opostos, ambos dentro de lava, e dar um bloco de ouro a cada um. Em alternativa, pousa um Bloco de Carniça encostado à lava: os adultos domesticados vão comê-lo quando quiserem e ficam prontos para acasalar. A fêmea põe um ovo.
- **Caça**: os selvagens caçam galinhas, cabras, vacas e ovelhas e recuperam vida ao comer.
- **Escamas**: cada cor larga a sua escama.
- **Selas** de ferro (simples para 1 jogador, dupla para 2). Voo com energia: ao esgotar, o dragão plana até ao chão e tem de descansar. Tecla **R** para soprar enquanto montas.

## Controlos (dono)

- Clique com sela na mão: equipa a sela. Clique com a mão vazia: monta. Shift + clique com a mão vazia: tira a sela.
- Sem sela, clique com a mão vazia: senta/levanta.
- Montado no chão, o dragão anda; carrega no espaço para levantar voo. Tecla R: sopro.

## Encontrar ninhos

Os ninhos são estruturas, por isso funcionam com `/locate`:

```
/locate structure drakebound:green_dragon_nest          (Cavernas Verdejantes)
/locate structure drakebound:yellow_dragon_nest         (picos das montanhas)
/locate structure drakebound:red_dragon_nest            (Badlands)
/execute in minecraft:the_nether run locate structure drakebound:nether_elder_dragon_nest
/execute in minecraft:the_end run locate structure drakebound:ender_dragon_nest
```

O Dragão Saqueador não tem ninho: aparece em raids e postos avançados (`/locate structure minecraft:pillager_outpost`).
Os ninhos só existem em chunks novos, gerados já com o mod instalado.
