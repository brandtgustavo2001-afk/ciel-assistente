# Validação da prévia 0.1

## Correção de ativação em 29/10/2026

Defeito reproduzido fora do Android com o mesmo modelo Vosk: a API informou explicitamente que `ciel` e `siel` estão ausentes do vocabulário. A frase sintetizada “Ciel, aumente o volume” foi transcrita como “se aumente o volume”; “Ciel” isolado resultou em texto vazio. A antiga comparação de texto nunca ativava nesses casos.

A chamada passou a usar comparação acústica MFCC/DTW com três amostras locais do usuário; Vosk fica responsável apenas pelo comando após o sinal. A síntese de voz é ignorada durante a reprodução, com retomada ao terminar, em vez de espera fixa por tamanho de mensagem.

16 testes locais passaram: 5 comandos, 3 regras de atualização e 8 detecção acústica/persistência/segmentação. O teste de nome usa três velocidades de voz sintetizada para calibração e uma quarta para validação. Os testes também verificam rejeição de silêncio e frases diferentes, volume reduzido, preservação da chamada no recorte de áudio e arquivo de calibração corrompido. Lint e compilação debug concluídos sem erros.

Esses testes NÃO medem a taxa de erros em voz humana, ruído, reprodução de música nem o microfone/tela bloqueada do Poco. Não há aparelho conectado. Confirmar no telefone usando Calibrar Ciel e depois Ciel → sinal → Aumente o volume; usar Falar comando para isolar microfone/transcrição da chamada. A distribuição continua experimental.

## Atualizador adicionado em 29/10/2026

- Build assinado de release, lint e 8 testes locais concluídos com sucesso (5 comandos + 3 política de download).
- Testes rejeitam URLs sem HTTPS, hosts parecidos, credenciais em URL, portas inesperadas e tamanhos inválidos.
- Assinatura SHA-256 do certificado comparada ao APK 0.1: idêntica. Isso permite atualização sem desinstalar.
- Lint sem erros; avisos de internacionalização e versões mais novas de dependências, sem bloqueio do build.
- Nenhum aparelho conectado: confirmação de instalação, notificação e execução agendada não foram testadas no Poco. Conferir na primeira instalação.
- Workflow do GitHub publica somente após testes, lint, compilação e assinatura concluídos.

Em 29/10/2026, neste computador:

- Gradle 8.9 / JDK 17: `testDebugUnitTest assembleDebug lintDebug` concluído com sucesso.
- 5 testes de interpretação de comandos: zero falhas e zero erros.
- Lint: sem erros; avisos restantes de internacionalização de textos em português.
- Assinatura APK verificada com Android apksigner (v2).
- Conferido no APK o modelo português e a biblioteca nativa arm64-v8a.
- Nenhum aparelho conectado ao ADB: instalação, interface, microfone, detecção de Ciel, tela bloqueada e consumo de bateria NÃO foram testados em aparelho ou emulador.
- Nenhuma conta Spotify conectada: OAuth e reprodução NÃO foram testados de ponta a ponta.

Ver README para limitações e roteiro de teste. APK de desenvolvimento, não homologado para distribuição pública.
