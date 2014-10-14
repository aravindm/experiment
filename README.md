experiment
==========

java port of https://github.com/github/dat-science

Depends on codahale.metrics and apache.commons.logging

example usage
-------------
```java
Experiment<Integer, Boolean, Boolean> exp1 = new Experiment<>((a) -> a % 2 == 0, (a) -> a % 10 == 0, "exp1");
exp1.run(4);
exp1.run(20);
exp1.run(30);
exp1.run(8);
System.out.println(exp1.getAll());
```
