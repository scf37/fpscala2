# fpscala2
Production-grade todo app REST API in functional scala3

This is an effort to create high quality application using FP approach with cats.
As this is a learning project, custom code is preferred over existing FP libraries.

## Features so far

- functional modular system
- layered architecture (dao - service - controller - route)
- performance: 11k req/sec on my i7 for `GET /api/v1/items`
- configuration from multiple sources (including `--help` command line parameter)
- pure data access layer based on JDBC
- pure JDBC transaction management
- separate thread pool for JDBC code
- quality JSON handling and validation (with readable messages, including reason, line and column)
- input validation (using Validated)
- quality error handling (error classification, REST error messages)
- pure logging
- structured audit logging
- data access layer tests using embedded postgres
- tests in tagless final style
- dao layer tests for both embedded postgres and in-memory dao
- acceptance tests using embedded postgres
- web tests using in-memory dao implementation
- packaging and dockerization. Try it now: `docker run --rm scf37/fpscala2 --help`
- immuatble in-memory dao using applicative context

## Architecture

### Layers

Typical multi layer architecture consisting of:
- data access layer (DAO)
- service layer (busines logic, intended to be called from within application)
- controller layer (includes transaction boundary, intended to be exposed to external clients via REST API)
- route layer (converts incoming HTTP request to controller call)
- server layer (HTTP server)

### Effects

Effect is an abstraction of some computation. 
Every effect can be created from computation definition, usually function (lifted) 
and at some time later can be run, extracting computation result (evaluated)

Application uses three types of effects:
- Generic effect `F[_]`. 
  - This is generic F used for asynchonous code with lazy evaluation, e.g. cats IO or monix Task
- Abstract database effect `DbEffect[_]` concrete database effect `SqlEffect[F[_], ?]`
  - It wraps function `java.sql.Connection => T`. Usual synchronous database code takes this form. Instance of `java.sql.Connection` is needed to evaluate this effect. 
- application initialization effect `I[_]`
  - it wraps component initialization code. It is lazy so components will only be created on demand and caches its result to produce singletons
  
### Modules

Modules system used can be seen as extension of well-known cake pattern keeping its strengths 
    of ability to override any instance or to get any instance from assembled application.
    
In addition, it supports composition, precise explicit dependency management and lazy evaluation.     

## TODOs

- request context (including requestId for logging) using `Kleisli[F, Context, ?]` instead of F
- application statistics: query and transaction execution timings. Finagle also provides request timing stats, make it more explicit somehow?
- delayed logging - delay logging evaluation till end of request processing to decide log level based on response (e.g. enable debug logging for failed requests only)
- add scalacheck?
- add scalafmt?
- Add request flow control: timeouts, parallel request count limit
- cancellation on timeout? Does it make sense on JDBC? Will it improve behavior of overloaded app?
