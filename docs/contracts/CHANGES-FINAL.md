# ATLAS Contracts v0.2 — ajustes finais

Este pack preserva a proposta v0.2 e aplica apenas correções de consistência antes do início da implementação Java.

1. `InboundMessage` virou união discriminada por `kind`; cada mensagem exige exatamente o payload correspondente.
2. O linter resolve `criticality` herdada de `device.defaultCriticality` antes de validar actions duráveis.
3. O linter ganhou validações de keys duplicadas, coerência semântica de properties, `messageExamples` e timestamps reais.
4. `Location` não possui limite semântico fixo de profundidade; ciclos continuam inválidos.
5. Retenção/storage de séries temporais permanecem `TBD` até o MVP fornecer volume real; `deadband` não é assumido antes de existir no domínio.
6. README corrigido para listar todos os schemas do pack.

A próxima versão dos contratos deve nascer de problemas encontrados durante a implementação, não de nova revisão abstrata.
