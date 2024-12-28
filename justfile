# docs: https://github.com/casey/just
# default settings: load .env files, export all variables as env vars, and accept positional
# arguments
set dotenv-load
set export

default:
  @just --choose

@build +mvnArgs='':
  ./mvnw install -DskipTests -DskipChecks -T1C {{ mvnArgs }}

@rebuild: clean build

@lint +mvnArgs='':
  ./mvnw verify -DskipTests {{ mvnArgs }}

@format +mvnArgs='':
  ./mvnw process-sources -PautoFormat -T2C {{ mvnArgs }}

@clean +mvnArgs='':
  ./mvnw clean -T2C {{ mvnArgs }}

test +mvnArgs='':
 ./mvnw verify -DskipChecks -T1C {{ mvnArgs }}

@ut +mvnArgs='': (test "-DskipITs" mvnArgs)
@it +mvnArgs='': (test "-DskipUTs" mvnArgs)

@install +mvnArgs='':
  ./mvnw install -DskipTests -DskipChecks -T1C {{ mvnArgs }}

@gen +mvnArgs='':
  ./mvnw generate-sources compile -DskipChecks -T1C {{ mvnArgs }}

@reinstall: clean install
