# SSSL Doktor Backend
## Development
### Do it once
  - install java 8+
  - install sbt 1.3.10
  - install [Docker](https://www.docker.com/community-edition)
  - install docker-compose (for Mac and Windows it is installed automatically with docker)

### Everyday stuffs
#### Start everything
  1. start PostgreSQL server:
    - execute in console from project root `docker-compose up` or see make file
  1. Start backend
    - there is an example.env file with the dev config values, copy the content of that file into a .env file
    - execute in console from project root `sbt run`

#### Execute tests
  - make sure you started the PostgreSQL server
  - execute in console from project root
    `sbt check`

#### Formatter
  - the project has its own fmt conf in .scalafmt.conf
  - make sure your IntelliJ plugin recognizes that conf