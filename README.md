## A plugable solution for demultiplexing events from a single kafka topic and treat as individual handlers

This repository contains a 


1. Setup kafka local
    
   1- downland docker image from confluentic and set to branch 5.1.2


   2- https://docs.confluent.io/3.0.0/quickstart.html

2. Topic creation

./kafka-topics.sh --zookeeper localhost:2181 --create --topic userEntity --partitions 3 --replication-factor 1 --config cleanup.policy=compact --config min.cleanable.dirty.ratio=0.05 --config segment.ms=604800000
./kafka-topics.sh --zookeeper localhost:2181 --create --topic user --partitions 3 --replication-factor 1


3. Register avro schemas to schema registry
## User TOPIC
# UnaryKey
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UnaryKey\", \"doc\" : \"Avro schema for key definition\", \"fields\" : [ { \"name\": \"id\", \"type\" : \"string\" , \"doc\" : \"Key value\" } ] }" }' http://localhost:8081/subjects/user-key/versions

# UserCreateEvent
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserCreateEvent\", \"doc\" : \"Avro schema for User Create Event\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" }, { \"name\": \"email\", \"type\" : \"string\" , \"doc\" : \"Email of User\" } ]}" }' http://localhost:8081/subjects/user-value/versions

# UserUpdateEvent
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserUpdateEvent\", \"doc\" : \"Avro schema for User Update Event\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" } ]}" }' http://localhost:8081/subjects/user-value/versions

## UserEntity TOPIC
# UnaryKey
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UnaryKey\", \"doc\" : \"Avro schema for key definition\", \"fields\" : [ { \"name\": \"id\", \"type\" : \"string\" , \"doc\" : \"Key value\" } ] }" }' http://localhost:8081/subjects/userEntity-key/versions

#UserEntity
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserEntity\", \"doc\" : \"Avro schema for applying and saving all updates on a User\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" }, { \"name\": \"email\", \"type\" : \"string\" , \"doc\" : \"Email of User\" } ]}" }' http://localhost:8081/subjects/userEntity-value/versions

3. Create a consumer and push data




4. Create a producer and get data