# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a brewing platform built with **Scala 2.13** using **Play Framework 2.9.x** and **Domain-Driven Design (DDD)** with **CQRS** architecture. The application manages brewing ingredients (hops, malts, yeasts, aromas) with complete CRUD operations, AI-powered recommendations, and admin interfaces.

## Development Commands

### Database Setup
```bash
# Start PostgreSQL database
docker compose up -d db

# Run application (applies evolutions automatically)
sbt run
```

### Core Commands
```bash
# Run the application
sbt run

# Run tests
sbt test

# Compile only
sbt compile

# Run tests with detailed output  
sbt "testOnly * -- -oD"

# Run specific test
sbt "testOnly *YeastServiceSpec"

# Architecture verification (custom task)
sbt verifyArchitecture

# Generate docs (custom task)
sbt generateDocs
```

### Data Management
```bash
# Reseed database with CSV data
curl -X POST http://localhost:9000/api/reseed
```

### Frontend (Elm)
```bash
# Compile Elm to JavaScript bundle
cd elm
elm make src/Main.elm --optimize --output ../public/js/app.js
```

## Architecture Overview

### DDD/CQRS Structure
The codebase follows strict Domain-Driven Design with CQRS separation:

```
app/
├── domain/                 # Pure domain logic
│   ├── shared/            # Shared kernel (Value Objects, DDD base classes)
│   ├── hops/              # Hops bounded context
│   ├── malts/             # Malts bounded context  
│   ├── yeasts/            # Yeasts bounded context
│   ├── aromas/            # Aromas bounded context
│   └── admin/             # Admin bounded context
├── application/           # Application services (CQRS)
│   ├── commands/          # Command side (writes)
│   └── queries/           # Query side (reads)
├── infrastructure/        # Technical concerns (DB, external services)
├── interfaces/           # HTTP controllers, API interfaces
└── utils/                # Cross-cutting concerns
```

### Key Patterns
- **Aggregate Roots**: Each domain has aggregates with strong consistency boundaries
- **Value Objects**: Immutable objects for domain concepts (percentage, names, etc.)
- **Domain Events**: For decoupled communication between aggregates
- **Repository Pattern**: Abstract data access with Slick implementations
- **Command/Query Handlers**: Separate read/write operations

### Domain Contexts
1. **Hops**: Manages hop varieties, alpha acids, flavors, brewing characteristics
   - *Target*: Intelligence features for style recommendations, flavor profiling, smart substitutions
2. **Malts**: Manages malt types, colors, extract values, brewing properties
   - *Target*: Recipe building automation, color optimization, flavor balance predictions  
3. **Yeasts**: Manages yeast strains, fermentation properties, temperature ranges
   - *Status*: Advanced intelligence implemented (recommendations, predictions, alternatives)
4. **Aromas**: Manages flavor profiles and aroma characteristics
5. **Admin**: Administrative operations, CSV imports, data management

**Intelligence Objective**: Achieve feature parity across Hops, Malts, and Yeasts domains for consistent user guidance and recommendation quality.

## Database Configuration

The application uses PostgreSQL with Slick ORM:
- **Environment Variable**: Set `DATABASE_URL` for automatic configuration
- **Manual Config**: Uncomment `url/user/password` lines in `conf/application.conf`
- **Evolutions**: Database schema managed in `conf/evolutions/default/`
- **Seeds**: Reference data in `conf/seeds/reference.json`

## API Endpoints

The application exposes RESTful APIs:
- `/api/hops/*` - Public hops API
- `/api/malts/*` - Public malts API  
- `/api/yeasts/*` - Public yeasts API
- `/api/aromas/*` - Public aromas API
- `/api/admin/*` - Administrative APIs (secured)
- `/api/reseed` - Database reseeding endpoint

## Testing Strategy

### Test Structure
- **Unit Tests**: Domain logic and services (`test/domain/`)
- **Integration Tests**: Controllers and database (`test/integration/`)
- **Test Configuration**: Uses `application.test.conf`

### Running Tests
- Tests run in forked JVM for isolation
- ScalaTest with MockitoSugar for mocking
- Detailed output with `-oD` flag

## Key Dependencies

- **Play Framework**: Web framework and dependency injection
- **Slick**: Database access layer with PostgreSQL driver
- **Play-JSON**: JSON serialization/deserialization  
- **Guice**: Dependency injection container
- **ScalaTest**: Testing framework with Play integration
- **Slick-PG**: PostgreSQL extensions for Slick

## Important Notes

- **Architecture Verification**: Run `sbt verifyArchitecture` before major changes
- **DDD Boundaries**: Respect bounded context boundaries - don't cross-reference domain models
- **CQRS Separation**: Commands modify state, queries read state - keep them separate
- **Database Evolutions**: Use Play evolutions for schema changes
- **Security**: Admin endpoints require proper authentication/authorization
- **Production Criteria**: Every implementation must be extensible and production-complete
- **Intelligence Uniformity**: All ingredient domains (Hops, Malts, Yeasts) must provide the same level of intelligent recommendations and user guidance