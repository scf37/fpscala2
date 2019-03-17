# fpscala2
Production-grade todo app REST API in functional scala

This is an effort to create high quality application using FP approach with cats.
As this is a learning project, custom code is preferred over existing FP libraries.

Features so far:

- functional modular system
- layered architecture (dao - service - controller - route)
- configuration from multiple sources (including `--help` command line parameter)
- pure data access layer based on JDBC
- pure JDBC transaction management
- quality JSON handling and validation (with readable messages, including reason, line and column)
- input validation (using Validated)
- quality error handling (error classification, REST error messages)
- pure logging
- data access layer tests using embedded postgres
- acceptance tests using embedded postgres
- packaging and dockerization. Try it now: `docker run --rm scf37/fpscala2 --help`
