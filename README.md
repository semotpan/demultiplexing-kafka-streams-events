## A plugable solution for demultiplexing events from a single kafka topic and treat as individual handlers

This repository provide an implementation of a pattern for handling many events (demultiplexing) from one topic 
and aggregate result to a compacted topic (KTable)

Are situations (at least in my case) when it has to aggregate different events which have different (AVRO) schemas in the same result,
and, also, when the solution should be extensible for new events that have to be aggregated into the same entity based on key.
To have a robust solution which is easy to adjust, test, remove an handler, or add new one (with different a schema), would be 
nice to have a pattern that allow to have all these requirements.
I'd present an implementation for such a task.

Usually, when it comes to implement a new kafka-streams processor, which should process real-time events and aggregate to a single result (KTable) grouped by a specific key,
you may be noticed that it follows next steps: filtration, grouping by a new key, and aggregate the result based on created key, and, eventually send to a topic. 
Resulting of this description can be extracted in next pattern workflow:
* Check the Event can be handled
* Select and group by a new key is needed
* Aggregate new Event to the compacted result, which includes applying updates and validating 
result consistency

Next pice of code present you, how may look first implementation

        streamBuilder.stream("user")
            .filter((key, event) -> event instanceof UserCreateEvent || event instanceof UserUpdateEvent)
            .groupBy((key, event) -> changeKey(key, event))
            .aggregate(UserEntity::new, (key, event, entity) -> aggregate(key, entity, entity), Materialized.as("user-entity-cache"))
            .toStream()
            .to("userEntity");


and here it's **changeKey** method and **aggregate** method (or they can be as separate objects)

    private UnaryKey changeKey(UnaryKey key, SpecificRecord event) {
         // do stuff
    }
            
    private UserEntity aggregate(UnaryKey key, UserEntity entity, UserEntity entity1) {
        // do stuff
    }

As you may notice such an implementation has limitations for testing and extendability, to have a more testable and extensible solution 
let's try to implement this a bit different where we can have encapsulate handlers for each event and a class which will aggregate each event


Define a new interface which will be implemented for every event

   

     public interface EventHandler<E, D> {

         Class<E> eventClass();
     
         SpecificRecord changeKey(final SpecificRecord key, final E event);
     
         void handle(E event, D destination);
     
         default boolean filter(final E event) {
             return true;
         }
 
     }

Every event now can answer with what class it handles and how to change key and handle the payload, filter method is added 
in case of filtering specific events depends on internal state


and here is an implementation


        
       public final class UserCreateEventHandler implements EventHandler<UserCreateEvent, UserEntity.Builder> {
       
           @Override
           public Class<UserCreateEvent> eventClass() {
               return UserCreateEvent.class;
           }
       
           @Override
           public SpecificRecord changeKey(final SpecificRecord key, final UserCreateEvent event) {
               return key;
           }
       
           @Override
           public void handle(final UserCreateEvent event, final UserEntity.Builder destination) {
               destination.setUserId(event.getUserId())
                       .setFirstName(event.getFirstName())
                       .setLastName(event.getLastName())
                       .setEmail(event.getEmail());
           }
       }
       

The aggregator handler is responsible for managing all incoming events, and it looks

        public final class EventAggregator<K extends SpecificRecord, D extends SpecificRecord> {
        
            private final Map<Class<? extends SpecificRecord>, EventHandler> handlerProvider;
            private final Function<D, RecordBuilder<D>> destinationBuilder;
            private final Predicate<D> eventProcessingValidator;
        
            public EventAggregator(final List<EventHandler> handlers,
                                   final Function<D, RecordBuilder<D>> destinationBuilder,
                                   final Predicate<D> eventProcessingValidator) {
        
                this.handlerProvider = Objects.requireNonNull(handlers).stream()
                        .collect(Collectors.toMap(EventHandler::eventClass, Function.identity()));
        
                this.destinationBuilder = Objects.requireNonNull(destinationBuilder);
                this.eventProcessingValidator = Objects.requireNonNull(eventProcessingValidator);
            }
        
            public boolean filter(final K key, final SpecificRecord event) {
                return null != event && handlerProvider.containsKey(event.getClass()) && handlerProvider.get(event.getClass()).filter(event);
            }
        
            public K changeKey(final K key, final SpecificRecord event) {
                return (K) handlerProvider.get(event.getClass()).changeKey(key, event);
            }
        
            public D aggregate(final K key, final SpecificRecord event, final D destination) {
                log.info("Processing event class: '{}' value: {}", event.getClass().getSimpleName(), event);
                final RecordBuilder<D> builder = destinationBuilder.apply(destination);
        
                try {
                    handlerProvider.get(event.getClass()).handle(event, builder);
                    return builder.build();
                } catch (final AvroRuntimeException ignored) {
        
                    // validate previous state if fails delete created entity
                    if (!eventProcessingValidator.test(destination)) {
                        log.error("Error processing event {}", event);
                        return null;
                    }
        
                    // get previous state
                    return destination;
                }
            }
        }
        
it accepts a list of handlers and a factory method for creating a entity builder, third param is an predicate to validate the result. 
AVRO builders can validate the created/updated object without sending it to schema registry.


Now to add a new event, you just need to implement a handler and register the handler for event aggregator, config class looks:

        
        @Bean
        public EventAggregator<UnaryKey, UserEntity> userEventAggregator() {
            return new EventAggregator<>(eventHandlerList(), userEntity -> UserEntity.newBuilder(), userEntity -> null != userEntity.getUserId());
        }
        
        private List<EventHandler> eventHandlerList() {
            final List<EventHandler> eventHandlers = new ArrayList<>(2);
            eventHandlers.add(new UserCreateEventHandler());
            eventHandlers.add(new UserUpdateEventHandler());
            return eventHandlers;
        }



### To start the application locally follow the steps
**1. Setup kafka cluster (including schema registry)**

   The easiest way to use docker (it means you should have docker installed) and clone 
   the confluent examples
   
   `git clone https://github.com/confluentinc/cp-docker-images.git`
   
   Checkout to branch `5.1.2-post`  
   
   access the folder `cp-docker-images/examples/cp-all-in-one` 
    
   and run `docker-compose up -d` in terminal

**2. Topic creation**
 
 To be able to create topics in kafka, there are two ways, either using Control Center (_localhost:9021_) or using utilities provided by confluent or kafka, the easiest way downland quick start from _https://docs.confluent.io/3.0.0/quickstart.html_

Once we are using kafka-topics utility then execute following commands:

- `./kafka-topics.sh --zookeeper localhost:2181 --create --topic userEntity --partitions 3 --replication-factor 1 --config cleanup.policy=compact --config min.cleanable.dirty.ratio=0.05 --config segment.ms=604800000`

- `./kafka-topics.sh --zookeeper localhost:2181 --create --topic user --partitions 3 --replication-factor 1`

As you may noticed there are created two topics, one topic has _cleanup.policy=compact_ and another has _cleanup.policy=delete_ which is default

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

and now you can send events to the user topic to see the result :)