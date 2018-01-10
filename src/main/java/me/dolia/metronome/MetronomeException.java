package me.dolia.metronome;

public class MetronomeException extends RuntimeException {

  public MetronomeException() {
  }

  public MetronomeException(String message) {
    super(message);
  }

  public MetronomeException(String message, Throwable cause) {
    super(message, cause);
  }
}
