package co.phoenixlab.discord;

@FunctionalInterface
public interface Command {

    void handleCommand(MessageContext context, String args);

}
