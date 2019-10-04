## A plugable solution for demultiplexing events from a single kafka topic and treat as individual handlers

This repository provide an implementation of a pattern for handling many events (demultiplexing) from one topic 
and aggregate result to a compacted topic (KTable)


The pattern workflow is following:
* Check the Event can be handled
* Select and group by a new key is needed
* Aggregate new Event to the compacted result, which includes applying updates and validating 
result consistency


### To start the application locally follow the steps
**1. Setup kafka cluster (including schema registry)**

   The easiest way to use docker (it means you should have docker installed) and clone 
   the confluent examples
   
   `git clone https://github.com/confluentinc/cp-docker-images.git`
   
   Checkout to branch `5.1.2-post`  access the folder `cp-docker-images/examples/cp-all-in-one` 
    and run `docker-compose up -d` in terminal

**2. Topic creation**
 
 To be able to create topics in kafka, there are two ways, either using Control Center (_localhost:9021_) or using utilities provided by confluent or kafka, the easiest way downland quick start from _https://docs.confluent.io/3.0.0/quickstart.html_

Once we are using kafka-topics utility then execute following commands:

- `./kafka-topics.sh --zookeeper localhost:2181 --create --topic userEntity --partitions 3 --replication-factor 1 --config cleanup.policy=compact --config min.cleanable.dirty.ratio=0.05 --config segment.ms=604800000`

- `./kafka-topics.sh --zookeeper localhost:2181 --create --topic user --partitions 3 --replication-factor 1`

As you may noticed there are created two topics, one topic has _cleanup.policy=compact_ and one has _cleanup.policy=delete_ which is default

**3. Register avro schemas to schema registry**

There is used AVRO for serialization, it provide a way to validate produced events to be according the schema before sending, building objects using generated builders


To have registered schemas to schema registry they should be posted, following request push schemas to schema registry:

**Topic - user**

key: `curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UnaryKey\", \"doc\" : \"Avro schema for key definition\", \"fields\" : [ { \"name\": \"id\", \"type\" : \"string\" , \"doc\" : \"Key value\" } ] }" }' http://localhost:8081/subjects/user-key/versions`

UserCreateEvent: `curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserCreateEvent\", \"doc\" : \"Avro schema for User Create Event\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" }, { \"name\": \"email\", \"type\" : \"string\" , \"doc\" : \"Email of User\" } ]}" }' http://localhost:8081/subjects/user-value/versions`

UserUpdateEvent: `curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserUpdateEvent\", \"doc\" : \"Avro schema for User Update Event\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" } ]}" }' http://localhost:8081/subjects/user-value/versions`

**Topic - userEntity**
key: `curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UnaryKey\", \"doc\" : \"Avro schema for key definition\", \"fields\" : [ { \"name\": \"id\", \"type\" : \"string\" , \"doc\" : \"Key value\" } ] }" }' http://localhost:8081/subjects/userEntity-key/versions`

UserEntity: `curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{ "schema": "{ \"type\" : \"record\", \"namespace\" : \"com.semotpan.avro\", \"name\" : \"UserEntity\", \"doc\" : \"Avro schema for applying and saving all updates on a User\", \"fields\" : [ { \"name\": \"user_id\", \"type\" : \"string\" , \"doc\" : \"Unique user ID\" }, { \"name\": \"first_name\", \"type\" : \"string\" , \"doc\" : \"First Name of User\" }, { \"name\": \"last_name\", \"type\" : \"string\" , \"doc\" : \"Last Name of User\" }, { \"name\": \"email\", \"type\" : \"string\" , \"doc\" : \"Email of User\" } ]}" }' http://localhost:8081/subjects/userEntity-value/versions`

**4. Run application**

Compile and create artifact

`mvn clean package`

Run app

`java -jar target/demultiplexing-kafka-streams-events-1.0-SNAPSHOT.jar`