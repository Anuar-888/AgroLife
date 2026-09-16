package kz.spelost.agroapp.model;

public class ChatMessage {
    public enum Sender { USER, BOT }

    public final Sender sender;
    public String text;
    public boolean pending;

    public ChatMessage(Sender sender, String text, boolean pending) {
        this.sender = sender;
        this.text = text;
        this.pending = pending;
    }
}
