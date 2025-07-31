# API Design Principles

This document outlines the core principles guiding the design of our API. Our aim is to create a robust, reusable, and efficient interface that serves various EM digital product teams, preventing redundant development efforts.

## Core principles

To achieve application agnosticism and promote consistency, this API strictly adheres to RESTful principles.

- Everything is a Resource
  - All relevant data entities are treated as resources, uniquely identified by URLs.

- Statelessness
  - Each request from a client to the server must contain all the information necessary to understand and fulfill the request. The server will not store any client context between requests.

- Layered System
  - Clients should not need to know whether they are connected directly to the end server or an intermediary. In our architecture, this API acts as an intermediary layer between EM digital products and the underlying EM data store, abstracting data access details.

- Read-Only Operations
  -  This API will exclusively support read-only operations, meaning only GET requests are permitted. This simplifies the API surface and focuses on data retrieval.

## Resource hierachy and identification

Our API defines resource paths based on the uniqueness of their identifiers, aiming for clarity, conciseness, and flexibility.

### Top-Level Resources (Globally Unique IDs)

Any resource with an identifier that is globally unique across the entire system will be treated as a top-level resource. This allows for direct access to the resource without needing its parent's context in the URL, even if logically it's a child of another resource.

Example: If a `Child` has a unique childId (e.g., a UUID), it can be accessed directly.

```
GET /children/{childId}
```

### Nested Resources (Contextually Unique IDs)

If a resource's identifier is only unique within the context of its parent, the resource path must include its parent's identifier to ensure unambiguous identification.

Example: If a `Child` resource has an id that is only unique to a specific `Parent`, you would need the parentId to identify it.

```
GET /parents/{parentId}/children/{childId}
```

This approach results in shorter, cleaner, and more intuitive resource paths where possible, and allows for the decoupling of child resources from their parents when the parent's identifier isn't relevant to the current operation.


## Preventing Round Trips (Data Embedding)

To optimize client-server interactions and prevent the inefficient "N+1 problem," our API supports the embedding of child resources within parent responses.

By default, all endpoints will return a lean response, including only the primary resource's data without any embedded children. Clients can explicitly request embedded children using a dedicated query parameter.

For example, in a scenario where a client needs to find all children for parents whose name matches a specific search term:

- Without embedding:
  - The client would first submit a request to find `Parent` resources matching the search term
    ```
    GET /parents?name=John
    ```
  - With the resulting list of parents, the client would then have to make an additional request for each parent to get their children
    ```
    GET /parents/{parentId1}/children
    GET /parents/{parentId2}/children
    ...
    GET /parents/{parentIdN}/children
    ```
- With embedding:
  - The client can make a single request, asking the API to embed the `Child` resources directly:
    ```
    GET /parents?name=John&include_children=true
    ```
  - Example response:
    ```json
    [
      {
        "id": "person1",
        "name": "John Smith",
        "children": [
          {
            "id": "child1"
          }
        ]
      },
      {
        "id": "person2",
        "name": "John Doe",
        "children": [
          {
            "id": "child2"
          }
        ]
      }
    ]
    ```

This approach significantly reduces network round trips, improving performance and simplifying client-side data handling.

