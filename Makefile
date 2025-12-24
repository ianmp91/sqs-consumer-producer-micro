# Makefile (en el directorio raíz del proyecto)

include makefile.base

.PHONY: all build clean test publish bootRun bootJar

# Tarea por defecto que llama a la tarea gradle_build definida en la base
all: build

# Alias simples dentro de make para mayor comodidad
# Estos alias llaman a las reglas definidas en makefiles.base

build: gradle_build

clean: gradle_clean

test: gradle_test

publish: gradle_publish

bootRun: gradle_bootRun

bootJar: gradle_bootJar

# Puedes añadir otras tareas específicas aquí si es necesario
run:
	$(GRADLE_WRAPPER) run
