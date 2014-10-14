experiment
==========

Useful to test changes(both performance and correctness) with production requests.
Always returns responses from the old flow and tests the performance/correctness of the new flow asynchronously.

java port of https://github.com/github/dat-science

Depends on codahale.metrics and apache.commons.logging

example usage
-------------
```java
Function<Integer,Boolean> oldFlow = (a) -> a % 2 == 0;
Function<Integer, Boolean> newFlow = (a) -> a % 10 == 0;
Experiment<Integer, Boolean, Boolean> exp1 = new Experiment<>(oldFlow, newFlow, "exp1");
exp1.run(4);
exp1.run(20);
exp1.run(30);
exp1.run(8);
System.out.println(exp1.getAllMetrics())
```
