# ATLAS — Pack de contratos v0.2

Oito arquivos de contrato/política. Seis são JSON Schema (draft 2020-12) com exemplos válidos embutidos; dois são **dados/política**, não schema. O `lint.py` é a ferramenta de validação do pack.

| Arquivo | Tipo | O que é |
|---|---|---|
| `vocabulary-v0.2.json` | dados | Fonte única de `semanticType`, `unit`, `deviceClass`, `valueType` e eventos de sistema |
| `environment-v0.2.json` | schema | Environment + árvore recursiva de Location |
| `device-v0.2.json` | schema | Device com descritores tipados de property, action e evento |
| `property-v0.2.json` | schema | PropertyDescriptor + PropertyState |
| `command-v0.2.json` | schema | Comando com ciclo de vida completo |
| `event-v0.2.json` | schema | Evento de sistema e evento semântico |
| `adapter-api-v0.2.json` | schema | Fronteira Core ↔ adapters/Lab, incluindo mensagens inbound e dispatch |
| `persistence-boundary-v0.2.json` | política | O que é durável, derivado, efêmero, secreto; restart e migração |
| `lint.py` | ferramenta | Valida schemas, exemplos e **consistência cruzada** entre arquivos |

```
pip install jsonschema && python3 lint.py
```

O linter faz o que o JSON Schema sozinho não faz: confere que todo `semanticType`, `unit` e `deviceClass` existe no vocabulário, que `writeAction`/`cancelAction`/`affectsProperties` apontam para coisas reais, que action durável e crítica tem cancelamento, que comando terminal tem `result`, que a árvore de locations não tem ciclo. Vale plugar no CI — contrato validado só por leitura humana apodrece.

### Ajustes finais deste pack

- `InboundMessage` é uma união discriminada real: cada `kind` exige exatamente seu payload correspondente.
- A criticidade efetiva de uma action considera herança de `device.defaultCriticality`; o linter aplica a regra de cancelamento também nesse caso.
- O linter verifica duplicidade de keys, coerência `semanticType`/`valueType`/dimensão de unidade e valida os `messageExamples` do Adapter API.
- A árvore de `Location` continua recursiva sem limite semântico fixo de profundidade; ciclos continuam proibidos.
- Storage, retenção e otimizações de séries temporais permanecem `TBD` até o MVP produzir dados reais.


---

## O que mudou em relação à v0.1

**1. Property não tem mais id global.** Era o problema mais caro: `device` implicava chave composta, `property` tinha `id`, `event` dependia do id. Agora o endereço é `(deviceId, key)`, e `prop_temperature_cozinha` — que embutia a localização e passaria a mentir no dia em que o sensor mudasse de cômodo — deixou de existir.

Um `key` é único dentro do device; `semanticType` classifica; `name` é humano e renomeável. É isso que permite um termostato ter `ambient_temperature` e `target_temperature`, ambos com `semanticType: "temperature"`.

**2. Actions viraram descritores tipados.** `["set_brightness"]` não dizia o que a action recebe. Agora há parâmetros com tipo, unidade, faixa e obrigatoriedade — o que destrava validação, renderização de UI, introspecção por IA, e o limite máximo de uma bomba.

**3. Comando durável existe.** O ciclo da v0.1 parava em `CONFIRMED`, que para `start_irrigation(10min)` significa apenas "a bomba ligou". Agora: `EXECUTING` → `COMPLETED` para actions com `durable: true`, mais `CANCELLED`, `EXPIRED` e `UNKNOWN_OUTCOME`. `cancelAction` é obrigatório em action durável e crítica.

**4. `lifecycle` e `health` são enums separados.** `RUNNING` + `UNHEALTHY` — adapter no ar, broker caído — era inexprimível na v0.1 e é exatamente o estado que você precisa ver.

**5. O vocabulário existe.** Era o item que faltava e o que apodrece mais rápido: sem ele, o terceiro adapter escreve `celsius`, o quarto escreve `C`, e ninguém percebe até doer.

**Além disso:** `automation.evaluated` (o evento que registra uma automação que avaliou e **não** agiu — sem ele *"por que a bomba não ligou hoje?"* é irrespondível); `loopGuard` por janela deslizante, porque `depth` sozinho não pega o ciclo A→B→A que reseta a profundidade; `Environment`/`Location` com árvore recursiva, que era referenciada e nunca definida; séries temporais na fronteira de persistência; reconciliação de comando condicionada à criticidade; e regra de migração de schema decidida antes do primeiro evento gravado.

---

## Regras transversais

**IDs são opacos e imutáveis.** Prefixo + ULID. Nunca embutir em um `id` nada que descreva a coisa — o que descreve vai em `name`, que é mutável. `dev_01JQ4K5XYZ` está certo; `prop_temperature_cozinha` estava errado.

**`unit` é sempre obrigatório e nunca nulo.** Grandeza adimensional usa `"NONE"`. Em Java isso evita a inconsistência entre campo ausente e campo null, e o `NullPointerException` que vem depois.

**Autoridade da unidade:** o `PropertyDescriptor` manda. O `Event` carrega uma cópia **de propósito** — evento é registro imutável e precisa ser legível daqui a três anos, mesmo que o descriptor tenha mudado. O `PropertyState` **não** carrega unidade: é sempre lido junto do descriptor. Isso reduz de quatro cópias para duas, e a que restou é intencional.

**Os três eixos de estado são ortogonais.** `availability` (consigo falar com o dispositivo?), `freshness` (quão antiga é a leitura?), `confidence` (o dispositivo confirmou ou estou supondo?). Dá para ter `CONFIRMED` + `STALE` e `ASSUMED` + `FRESH`.

**Frescor é por property.** Um sensor de porta em `ON_CHANGE` silencioso há três dias está correto. Um sensor de temperatura em `PERIODIC` silencioso há uma hora está suspeito.

**`traceId` nasce na causa-raiz** — evento físico, ação de usuário, disparo de timer — e propaga por toda a cadeia descendente. Nunca é regenerado no meio.

**Comando de automação exige `causality.cause`.** É esse elo que torna a detecção de loop e o rastro causal possíveis, e o schema obriga.

---

## A fronteira do Lab

`adapter-api-v0.2.json` é o **único** contrato pelo qual o ATLAS Lab fala com o Core. Em Gradle:

```
atlas-adapter-api      ← só interfaces e DTOs
atlas-core             → depende de atlas-adapter-api
atlas-lab-*            → depende de atlas-adapter-api  (NUNCA de atlas-core)
```

Se o `build.gradle` do Lab não consegue enxergar `atlas-core`, a promessa do §28.8 do briefing ("se der errado, removemos sem destruir o Core") passa a ser verdadeira por compilação em vez de por disciplina. E você vira o primeiro consumidor externo do seu próprio contrato de adapter — se ele for ruim, dói em você enquanto ainda é barato consertar.

---

## Usando no projeto Java

Os schemas usam `$ref` entre arquivos via `$id` (`https://schemas.atlas.local/v0.2/...`). Esses URIs **não são para resolver na rede** — registre os arquivos localmente no validador.

Com `com.networknt:json-schema-validator`:

```java
var mapping = Map.of(
    "https://schemas.atlas.local/v0.2/device.json",      "classpath:schemas/device-v0.2.json",
    "https://schemas.atlas.local/v0.2/property.json",    "classpath:schemas/property-v0.2.json",
    "https://schemas.atlas.local/v0.2/command.json",     "classpath:schemas/command-v0.2.json",
    "https://schemas.atlas.local/v0.2/event.json",       "classpath:schemas/event-v0.2.json",
    "https://schemas.atlas.local/v0.2/adapter-api.json", "classpath:schemas/adapter-api-v0.2.json",
    "https://schemas.atlas.local/v0.2/environment.json", "classpath:schemas/environment-v0.2.json"
);

var factory = JsonSchemaFactory
    .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
    .schemaMappers(m -> m.mappings(mapping))
    .build();
```

Coloque os arquivos em `src/main/resources/schemas/`.

**Um lembrete que vale mais que o resto:** o domínio (`Device`, `Property`, `PropertyState`, `Command`, `Event`) deve ser **Java puro — zero anotação de Spring ou JPA**. Spring existe só nas bordas: HTTP, MQTT, persistência. É a diferença entre um sistema que você ainda entende no ano 3 e um que você não entende.

E toda mutação de estado atravessa **uma única fila serializada**. Callbacks MQTT, timers, requisições HTTP e o motor de automação vão todos querer mutar o mesmo registry — e bug de concorrência em sistema físico é intermitente, dependente de timing e acontece às 3h da manhã.

---

## O que continua deliberadamente indefinido

Automação (DSL, superfície de autoria, motor) · sistema de módulos e carga de plugins · permissões · UI · federação entre nodes · a camada de IA inteira.

Isso é intencional, e a v0.1 acertou ao não escrever automação. A DSL você só descobre depois de escrever vinte automações à mão.

---

## Próximo passo

Codificar. A v0.3 destes contratos deve nascer de um problema encontrado no código, **não** de mais uma rodada de leitura — você descobre o que falta em `set_brightness` numa tarde escrevendo a UI mínima, e essa descoberta vale mais que qualquer revisão.

O MVP não mudou: 1 Environment, adapter MQTT, um punhado de properties, estado, eventos, uma ou duas automações reais, interface mínima. E o marco que importa:

> Se eu desligar o ATLAS, alguém em casa reclama.
