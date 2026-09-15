# AutoPTU — Slice 2: HUD y tutorial de batallas

Versión preliminar para Minecraft Java 1.21.1 / Java 21 / Fabric.
El archivo `.jar` es un mod: no se ejecuta con doble clic como un EXE.

## Mejoras

- Barras de vida animadas con estela ámbar de daño; los números siguen mostrando PS reales.
- Panel del bando activo destacado, avisos de turno y estados traducidos al español.
- Tutorial ilustrado de seis pasos: `/autoptu battle help` y botón `?` en preparación.
- Guía HTML offline con generador de comandos y validación de semilla de 64 bits.
- Navegación con teclado, texto desplazable y ejemplos que se copian sin ejecutarse.

## Instalación y comandos

Usá un perfil separado con Minecraft 1.21.1, Java 21, Fabric Loader 0.18.2 o posterior
compatible, Cobblemon 1.8.0+1.21.1, Fabric API 0.116.11+1.21.1 y Fabric Language Kotlin
1.13.6+kotlin.2.2.20. Copiá el JAR AutoPTU en `mods`. Si ya tenías AutoPTU, reemplazá
el JAR anterior; no dejes dos versiones. No actualices un modpack Cobblemon 1.7.1
con este JAR sin resolver antes sus incompatibilidades.

En un mundo creativo superplano, con comandos habilitados y espacio despejado:

```text
/autoptu battle help
/autoptu battle practice
/autoptu admin battle play charmander pikachu
/autoptu admin battle play lucario gengar distance 12345
```

Formato: `/autoptu admin battle play <tu_especie> <rival> [escenario] [semilla]`.
Escenarios: `training`, `duel`, `distance`, `endurance`.
B abre acciones, Enter confirma la previsualización, Backspace cancela, H abre el informe.
`/autoptu admin battle stop` cierra tu sesión.

## Validación y límites

15 pruebas enfocadas aprobadas; compilación, remapeado y empaquetado local exitosos.
No se ha registrado una partida gráfica manual completa de esta versión.

Práctica 1 contra 1 contra un rival automático. Las especies eligen los modelos;
las estadísticas son de escenario y hay tres ataques propios. No incluye PvP,
captura, recompensas de campaña, movimientos nativos, habilidades ni objetos.
El servidor requiere OP para iniciar práctica. El mod no aplana el terreno.

Los assets de esta publicación son el mod y la guía HTML; las dependencias no están
incluidas en el JAR publicado. El ZIP local generado por `packagePracticeBattle`
sí contiene los cuatro mods y ambas guías.
