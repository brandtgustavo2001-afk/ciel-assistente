# Validação da prévia 0.1

Em 29/10/2026, neste computador:

- Gradle 8.9 / JDK 17: `testDebugUnitTest assembleDebug lintDebug` concluído com sucesso.
- 5 testes de interpretação de comandos: zero falhas e zero erros.
- Lint: sem erros; avisos restantes de internacionalização de textos em português.
- Assinatura APK verificada com Android apksigner (v2).
- Conferido no APK o modelo português e a biblioteca nativa arm64-v8a.
- Nenhum aparelho conectado ao ADB: instalação, interface, microfone, detecção de Ciel, tela bloqueada e consumo de bateria NÃO foram testados em aparelho ou emulador.
- Nenhuma conta Spotify conectada: OAuth e reprodução NÃO foram testados de ponta a ponta.

Ver README para limitações e roteiro de teste. APK de desenvolvimento, não homologado para distribuição pública.
