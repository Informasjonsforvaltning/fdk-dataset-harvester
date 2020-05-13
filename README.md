# FDK dataset harvester

## Requirements
- maven
- java 8
- docker
- docker-compose

## Run tests
```
% mvn verify
```

## Run locally
```
docker-compose up -d
mvn clean compile
mvn exec:java -Dspring.profiles.active=develop
```

Then in another terminal e.g.
```
% curl http://localhost:8080/catalogs
% curl http://localhost:8080/datasets
```
