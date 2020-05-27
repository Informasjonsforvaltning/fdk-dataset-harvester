# FDK dataset harvester

## Requirements
- maven
- java 8
- docker
- docker-compose

## Run tests
Make sure you have an updated docker image with the tag "eu.gcr.io/digdir-fdk-infra/fdk-dataset-harvester:latest"
```
mvn verify
```
Optionally, if you want to use an image with another tag:
```
mvn verify -DtestImageName="<image-tag>"
```

## Run locally
```
docker-compose up -d
mvn spring-boot:run -Dspring.profiles.active=develop
```

Then in another terminal e.g.
```
% curl http://localhost:8080/catalogs
% curl http://localhost:8080/datasets
```

## Datastore
To inspect the Fuseki triple store, open your browser at http://localhost:3030/fuseki/
