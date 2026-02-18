# News Retrieval System

A contextual news data retrieval system built with Spring Boot that leverages LLM integration for natural language query processing, location-based trending features, and multiple retrieval strategies.

## Prerequisites
- Java 21
- Maven
- PostgreSQL
- Redis
- LLM API access (HuggingFace or OpenAI-compatible)

## Steup redis and postgresssql
Execute below command in terminal 

1. docker run -d   --name redis   -p 6379:6379   redis redis-server --requirepass my-redis
2. docker run --name postgres-news   -e POSTGRES_USER=news   -e POSTGRES_PASSWORD=news123   -e POSTGRES_DB=newsdb   -p 5432:5432   -d postgres:15

## To Run application locally 
In Run Configuration 
progrument argument : --spring.config.location=configs/application.yml
main class : com.news.retrieval.NewsRetrievalApplication
select java 21 jdk 

<img width="1265" height="760" alt="image" src="https://github.com/user-attachments/assets/f33a44eb-5ab3-427d-8576-142c82fc81d1" />

### 3. Configuration

Update `configs/application.yml` with your settings:

Set LLM key and model 

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The server will start on port **9097**.

### News Retrieval

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/news/query` | Natural language query processing |
| GET | `/api/v1/news/category?category={category}` | Get articles by category |
| GET | `/api/v1/news/search?query={query}` | Keyword search |
| GET | `/api/v1/news/score?threshold={threshold}` | Get articles by relevance score |
| GET | `/api/v1/news/source?source={source}` | Get articles by source |
| GET | `/api/v1/news/nearby?lat={lat}&lon={lon}` | Location-based articles |


### Trending

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/news/trending?lat={lat}&lon={lon}&limit={limit}` | Get trending articles near location |
| POST | `/api/v1/news/trending/simulate?count={count}` | Simulate user events for testing |


### Data Ingestion

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/ingest` | Ingest news data from configured source |

### Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check endpoint |

## Usage Examples

### Natural Language Query

```bash
curl -X POST http://localhost:9097/api/v1/news/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the latest technology news about AI?",
    "latitude": 40.7128,
    "longitude": -74.0060
  }'
```

### Search by Category

```bash
curl "http://localhost:9097/api/v1/news/category?category=technology"
```

### Keyword Search

```bash
curl "http://localhost:9097/api/v1/news/search?query=artificial%20intelligence"
```

### Get Nearby Articles

```bash
curl "http://localhost:9097/api/v1/news/nearby?lat=40.7128&lon=-74.0060"
```

### Get Trending Articles

```bash
curl "http://localhost:9097/api/v1/news/trending?lat=40.7128&lon=-74.0060&limit=10"

## Data Models

### NewsArticle
- `id` - Unique identifier
- `title` - Article title
- `description` - Article content/description
- `url` - Source URL
- `publicationDate` - Publication timestamp
- `sourceName` - News source name
- `relevanceScore` - Relevance score (0.0 - 1.0)
- `latitude`, `longitude` - Geographic coordinates
- `categories` - Associated categories (Many-to-Many)

### TrendingScore
- `geoCell` - Geographic cell identifier
- `articleId` - Associated article
- `score` - Trending score
- `updatedAt` - Last update timestamp

### UserArticleEvent
- `articleId` - Associated article
- `eventType` - Event type (VIEW: 1.0, CLICK: 3.0, SHARE: 5.0 weight)
- `latitude`, `longitude` - Event location
- `createdAt` - Event timestamp

## Test Cases
I have attached test case screenshot
