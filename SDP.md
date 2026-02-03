**Software Development Plan (SDP)**
*Version: 0.1"
*3-Feb-2026*

# **1. Coding Standard**
## **Naming & Declarations:**
### *Conventions:*
PascalCase for classes, camelCase for methods/variables, and UPPER_SNAKE_CASE for static final constants.

### *Magic Numbers:*
Avoid hardcoded literals; use named constants to improve maintainability.

### *Numeric Readability:*
Use underscores in long numeric literals (e.g., 1_000_000).

## **Object & Class Design:**
### *Encapsulation:*
Members are private by default. Adhere to SOLID principles.

### *Composition Over Inheritance:*
Favor "has-a" relationships to avoid the fragile base class problem. Use Interface-based programming to ensure software reuse and flexibility.

### *Immutability:*
Prefer final fields and Java Records for data carriers to ensure thread safety.

## **Modern Java Logic:**
### *Resources:*
Mandatory use of try-with-resources for AutoCloseable objects.

### *Collections:*
Use Lambdas/Streams for processing and Optional<T> to eliminate NullPointerExceptions.

### *Control Flow:*
Use Guard Clauses (early returns) to flatten nested if-else blocks and modern Switch Expressions (->) to avoid fallthrough errors.

## **Error Handling & Safety:**
### *Exceptions:*
Catch specific exceptions; never catch generic Exception or Throwable. No empty catch blocks.

### *Security:*
Sanitize inputs at trust boundaries and avoid logging sensitive PII data.


# **2. Code Verification**
## **Automated Testing**
### *Unit Testing:*
Built on the JUnit 5 framework.

### *Coverage Goal:*
Minimum 80% code coverage for all new modules.

### *Assessment:*
JaCoCo is used to actively assess coverage and fail builds that fall below the threshold.

## **Integration Testing:**
### *Location:* 
All integration tests reside in the src/integration/java source set.

### *Standards:*
ArchUnit enforces the use of @ExtendWith(MockitoExtension.class) to ensure strict stubbing and prevent brittle mocks.

## **Functional Testing:**
Verification of "Black Box" functionality against the criteria defined in GitHub Issues.

## **Test Isolation (Testcontainers Strategy)**

### *Ephemeral Environments:*
Use Testcontainers to spin up disposable instances of external dependencies (Databases, Brokers, etc.).

### *Dynamic Port Mapping:*
Hardcoded ports are prohibited. Use the Networking API to retrieve randomized host ports at runtime.

### *Data Purity:*
Implement @BeforeEach cleanup routines (e.g., truncating tables) to ensure shared containers do not leak state between tests.

### *Resource Reaping:*
Rely on the Ryuk sidecar container to clean up all Docker resources upon task completion.

# **3. Requirements & Project Management**
## **Requirements Management**
### *Single Source of Truth:*
GitHub Issues serve as the primary requirements repository.

### *Traceability:*
The GitHub Issue ID (e.g., #42) is the official Requirement ID. It must be referenced in Javadoc comments and Git commit messages.

### *Scoped Documentation:*
For complex logic, a [project/class]-requirements.md file is maintained in the repo to provide deep technical context, linked directly in the parent Issue.

## **Review and Analysis**
All project initiatives must undergo a formal review phase. No feature or project planning may proceed until requirements and architecture have been validated against the following criteria.

### **Requirements Review**
All requirements will be reviewed against the following Best Practices for Good Requirements:

#### *Atomic:*
Each requirement should specify a single function.

#### *Unambiguous:*
Requirements must be clear and subject to only one interpretation.

#### *Verifiable:*
Each requirement must be testable through Automated or Functional testing.

#### *Feasible:*
Requirements must be implementable within the constraints of the project.

#### *Necessary:*
Each requirement must provide documented value toward the project goal.

#### *Consistent:*
Requirements must not conflict with other documented requirements or standards.

#### *Verification Statement:*
All requirements will be reviewed against these criteria prior to proceeding to feature/project planning.

### **Architecture Review**
Prior to project planning, potential technical architectures will be reviewed to ensure the chosen path is the most efficient and sustainable.

#### *Evaluation Criteria:*
Architectures will be reviewed for long-term sustainability, performance, technical complexity, and estimated time to develop.

#### *Metric Integration:*
Complexity and time-to-develop data will drive the project planning phase via the Estimation & Metrics section.

#### *Formal Trade Studies:*
Competing architectures will be compared using weighted criteria and the formal trade study methodology when possible.

When a formal trade study isn't feasible, potential architectures will be compared with a documented list of Pros and Cons, specifically highlighting impacts on complexity and the development timeline.

#### *Minimum Documentation:*
Architecture reviews are documented in a centralized file: docs/arch/ARCH-REVIEW.md.
Gradle tasks will be utilized to verify the presence and "Last Modified" status of ARCH-REVIEW.md to ensure architectural alignment remains current as new requirements are added.

## **Estimation & Metrics**
### *Complexity Metrics:*
We use the cloc Gradle plugin for volume and PMD for Cyclomatic Complexity ratings.

### *Data-Driven Estimation:*
We compare the metrics of new tasks against historical data (e.g., "Project B is similar in complexity to Project A") using GitHub Project Custom Fields to track Complexity vs. Actual Development Time.

### *Local Visibility:*
A custom listIssues Gradle task invokes the GitHub CLI (gh) to display the active backlog in the developer console.

## **Task Lifecycle (Issue Template)**
Every task must progress through:
### *Analysis:*
Review requirements and technical constraints.
### *Planning:*
Estimate complexity based on past similar Issue IDs.
### *Development:*
Code implementation following Java standards.
### *Test:*
Successful execution of Unit and Integration suites.
### *Review & Cleanup:*
Refactor for debt and ensure requirements are met.

# **4. Development Roadmap**
## **Ingest & Rate:** 
### *Description:*
Populate GitHub Issues and assign complexity ratings.

## **Infrastructure Sync:**
### *Description:*
Verify Gradle build gates (Checkstyle, JaCoCo, ArchUnit).

## **Sprint Execution:**
### *Description:*
Iterative coding against the Task Lifecycle.

## **Verification & Closure:**
### *Description:*
Use linking keywords (closes #ID) to automate board movement upon successful build.

# **5. Common Pitfalls and Lessons Learned**
(Team-maintained log for project-specific issues and recurring roadblocks.)
[Entry 1]: (Reserved)
