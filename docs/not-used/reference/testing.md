---
title: Unit testing
parent: Reference
has_children: false
nav_order: 70
published: true
layout: default
---


# Unit testing Fluxtion
---

There are no special steps required to run event processors in a unit test or any other testing framework, they are 
normal java classes that can be used in any domain.

See the [developer workflow section](../gettingstarted/developer-workflow#unit-test) for an example of unit testing

{% highlight java %}

class CommandExecutorTest {
    @Test
    public void testPermission(){
        var processor = Fluxtion.interpret(new CommandExecutor(new CommandAuthorizerNode()));
        processor.init();

        CommandAuthorizer commandAuthorizer = processor.getExportedService();
        commandAuthorizer.authorize(new CommandPermission("admin", "shutdown"));
        commandAuthorizer.authorize(new CommandPermission("admin", "listUser"));
        commandAuthorizer.authorize(new CommandPermission("Aslam", "listUser"));
        commandAuthorizer.authorize(new CommandPermission("Puck", "createMischief"));

        LongAdder longAdder = new LongAdder();
        processor.onEvent(new AdminCommand("admin", "shutdown", longAdder::increment));
        Assertions.assertEquals(1, longAdder.intValue());

        processor.onEvent(new AdminCommand("Aslam", "listUser", longAdder::increment));
        Assertions.assertEquals(2, longAdder.intValue());

        processor.onEvent(new AdminCommand("Puck", "createMischief", longAdder::increment));
        Assertions.assertEquals(3, longAdder.intValue());

        processor.onEvent(new AdminCommand("Aslam", "shutdown", longAdder::increment));
        Assertions.assertEquals(3, longAdder.intValue());

        commandAuthorizer.removeAuthorized(new CommandPermission("Puck", "createMischief"));
        processor.onEvent(new AdminCommand("Puck", "createMischief", longAdder::increment));
        Assertions.assertEquals(3, longAdder.intValue());
    }
}

{% endhighlight %}
