# Week 1 API Design and Requirements Analysis

This folder contains the Week 1 Word report and supporting API contract.

Files:
- Week1_Ecommerce_API_Design.docx: required Word report for upload.
- openapi.yaml: OpenAPI 3.0.3 contract.
- submission-description.txt: description exceeding the 200-word form minimum.
- diagrams/: diagram images embedded in the report and editable Mermaid sources.

Decisions:
- API base path /api/v1; proposed backend Java with Spring Boot and PostgreSQL.
- JWT bearer authentication with CUSTOMER and ADMIN roles.
- INR amounts represented as integer paise.
- Order placement reserves inventory atomically.

Week 1 defines the design; it does not implement the backend service.
