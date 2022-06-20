# FDK dataset harvester

## Requirements
- maven
- java 17
- docker
- docker-compose

## Run tests
```
mvn verify
```

## Run locally
### docker-compose
```
docker-compose up -d --build
```

Then in the terminal e.g.
```
% curl http://localhost:8081/catalogs
```
### maven
```
docker-compose up -d rabbitmq
docker-compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=develop"
```

Then in another terminal e.g.
```
% curl http://localhost:8080/catalogs
```
